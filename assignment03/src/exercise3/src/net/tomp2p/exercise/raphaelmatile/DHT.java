package net.tomp2p.exercise.raphaelmatile.exercise2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.FutureSend;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.p2p.RequestP2PConfiguration;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

public class DHT {
	
	private int masterNodeId;
	
	public DHT(int masterNodeId) {
		this.masterNodeId = masterNodeId;
	}
	
	/**
	 * Puts the given value with the provided key on the provided peer
	 * 
	 * @param peer The peer to store the data on
	 * @param key The key of the data
	 * @param value The value of the data
	 * 
	 * @return FuturePut The future of the put operation
	 * 
	 * @throws IOException
	 */
	public FuturePut put(PeerDHT peer, Number160 key, PeerAddress value) throws IOException {
		Data data = new Data(value);
		FuturePut put = peer.put(key).data(data).start();
		
		// non blocking 
		put.addListener(new BaseFutureAdapter<FuturePut>() {

			@Override
			public void operationComplete(FuturePut future) throws Exception {
				if (future.isSuccess()) {
					System.out.println("[" + peer.peerID().intValue() + "]: stored (" + key.intValue() + ", " + value + ")");
				} else if (future.isFailed()) {
					System.out.println("[" + peer.peerID().intValue() + "]: failed to store (" + key.intValue() + ", " + value + ")");
				}
			}
		});
		
		return put;
	}
	
	/**
	 * Sends the given value to the provided peer from the provided sender.
	 * 
	 * @param sender The peer which sends the given value
	 * @param receiver The peer to which the value should be sent
	 * @param value The value to send
	 * 
	 * @return FutureSend The future of the send operation 
	 */
	public FutureSend send(PeerDHT sender, PeerAddress receiver, String value) {
		RequestP2PConfiguration config = new RequestP2PConfiguration(1, 10, 0);
		FutureSend send = sender.send(receiver.peerId()).object(value).requestP2PConfiguration(config).start();
	
		send.addListener(new BaseFutureAdapter<FutureSend>() {

			@Override
			public void operationComplete(FutureSend future) throws Exception {
				if (future.isSuccess()) {
					System.out.println("[" + sender.peerID().intValue() + "]: sent " + value + ".");
				} else if (future.isFailed()) {
					System.out.println("[" + sender.peerID().intValue() + "]: failed to sent. Message:  " + future.failedReason() + ".");
				}
			}
		});
		
		return send;
	}
	
	/**
	 * Gets the key value pair with the provided key on the provided peer
	 * 
	 * @param peer The peer which should fetch the key-value pair
	 * @param key The key of the pair which should be fetched
	 *  
	 * @return FutureGet The future of the get operation 
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public FutureGet get(PeerDHT peer, Number160 key) throws ClassNotFoundException, IOException {
		FutureGet get = peer.get(key).start();
		// get non blocking using event listener
		get.addListener(new BaseFutureAdapter<FutureGet>() {

			@Override
			public void operationComplete(FutureGet future) throws Exception {
				if (future.isSuccess()) {
					Object storedObject = future.data().object();
					System.out.println("[" + peer.peerID().intValue() + "]: received key value pair (" + key.intValue() + ", " + storedObject + ")");
				}
			}
		});
		
		return get;
	}

	/**
	 * Shuts down all peers in the provided list
	 * 
	 * @param peers
	 */
	public void shutDownPeers(List<PeerDHT> peers) {
		for (PeerDHT peer : peers) {
			peer.shutdown();
		}
	}
	
	/**
	 * Bootstraps all given peers to the master node
	 * 
	 * @param peers
	 */
	public void bootstrap(List<PeerDHT> peers) {
		for (PeerDHT peer : peers) {
			peer.peerBean().peerMap().peerFound(peers.get(this.masterNodeId).peerAddress(), null, null, null);
		}
		
		// add reply listener, so that send can complete successfully
		for (final PeerDHT peer : peers) {
            peer.peer().objectDataReply(new ObjectDataReply() {

                @Override
                public Object reply(PeerAddress sender, Object request)
                        throws Exception {
                    System.out.println("[" + peer.peerID().intValue() + "]: received message " + request + " from [" + sender.peerId().intValue() + "]");
                    return "";
                }
            });
        }
	}
	
	/**
	 * Set ups the given number of peers on the provided port.
	 * 
	 * @param nrOfPeers Amount of peers which should be set up
	 * @param port The port which should be used
	 * @return List<PeerDHT> The created and started peers
	 * @throws IOException
	 */
	public List<PeerDHT> setupDHT(int nrOfPeers, int port) throws IOException {
		List<PeerDHT> peers = new ArrayList<>();
		
		// add master peer
		peers.add(new PeerBuilderDHT(createAndStartPeer(this.masterNodeId, port)).start());
		
		for (int i=1; i<port; i++) {
			PeerBuilder peerBuilder = new PeerBuilder(new Number160(i+1));
			Peer peer = peerBuilder.masterPeer(peers.get(0).peer()).start();
			PeerBuilderDHT peerBuilderDHT = new PeerBuilderDHT(peer);
			PeerDHT peerDHT = peerBuilderDHT.start();
			
			peers.add(peerDHT);
		}
		
		return peers;
	}
	
	/**
	 * Creates a peer with the given id plus one listening on the given port id
	 * 
	 * @param peerId Peer id
	 * @param port Port on which the peer should listen on
	 * @return Peer The created peer
	 * @throws IOException
	 */
	public Peer createAndStartPeer(int peerId, int port) throws IOException {
		PeerBuilder peerBuilder = new PeerBuilder(new Number160(peerId + 1));
		peerBuilder.ports(port);
		Peer peer = peerBuilder.start();
		
		return peer;
	}
}
