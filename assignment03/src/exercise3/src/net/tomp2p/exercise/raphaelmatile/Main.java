package net.tomp2p.exercise.raphaelmatile.exercise2;

import java.io.IOException;
import java.util.List;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.FutureSend;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;

public class Main {

	public static final int MASTER_NODE_ID = 0;
	public static final int AMOUNT_OF_PEERS = 10;
	public static final int STORING_PEER_ID = 2;
	public static final int REQUESTING_PEER_ID = 5;
	
	public static final int PORT = 4001;
	

	public static void main(String[] args) {
			List<PeerDHT> peers;
			
			DHT dht = new DHT(MASTER_NODE_ID);
			
			try {
				peers = dht.setupDHT(AMOUNT_OF_PEERS, PORT);
				dht.bootstrap(peers);

				// store address from peer 
				Number160 key = new Number160(123);
				PeerAddress value = peers.get(STORING_PEER_ID).peerAddress();

				FuturePut put = dht.put(peers.get(STORING_PEER_ID), key, value);
				put.await(); // wait for async call to complete

				FutureGet get = dht.get(peers.get(REQUESTING_PEER_ID), key);
				get.await(); // wait to async call to complete
				
				PeerAddress address = (PeerAddress) get.data().object();
				FutureSend send = dht.send(peers.get(REQUESTING_PEER_ID), address, "Blub");
				send.await();
				
				dht.shutDownPeers(peers);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			

	}
}
