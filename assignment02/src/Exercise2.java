package net.tomp2p.exercise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FuturePut;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

/**
 * @author  Samuel von Baussnern
 * @author  Raphael Matile
 */
public class Exercise2 {
    
    private static final int MASTER_NODE_ID = 0;
    private static final int AMOUNT_OF_PEERS = 10;
    private static final int STORING_PEER_ID = 2;
    private static final int REQUESTING_PEER_ID = 5;
    
    private static final int PORT = 4001;
    

    public static void main(String[] args) {
            List<PeerDHT> peers;
            
            Number160 key = new Number160(123);
            
            try {
                peers = setupDHT(AMOUNT_OF_PEERS, PORT);
                bootstrap(peers);
                
                put(peers.get(STORING_PEER_ID), key, "Blub");
                get(peers.get(REQUESTING_PEER_ID), key);
                
                shutDownPeers(peers);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            

    }
    
    /**
     * Puts the given value with the provided key on the provided peer
     * 
     * @param peer The peer to store the data on
     * @param key The key of the data
     * @param value The value of the data
     * @throws IOException
     */
    public static void put(PeerDHT peer, Number160 key, String value) throws IOException {
        Data data = new Data(value);
        FuturePut put = peer.put(key).data(data).start();
        // wait for storing (blocking)
        put.awaitUninterruptibly();
        
        System.out.println("[" + peer.peerID().intValue() + "]: stored (" + key.intValue() + ", " + value + ")");
    }
    
    /**
     * Gets the key value pair with the provided key on the provided peer
     * 
     * @param peer The peer which should fetch the key-value pair
     * @param key The key of the pair which should be fetched 
     * @return Object The object which was stored 
     * 
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static Object get(PeerDHT peer, Number160 key) throws ClassNotFoundException, IOException {
        FutureGet get = peer.get(key).start();
        // wait for value (blocking)
        get.awaitUninterruptibly();
        
        Set<Entry<PeerAddress, Map<Number640, Data>>> replies = get.rawData().entrySet();
        Object storedObject = get.data().object();
        System.out.println("[" + peer.peerID().intValue() + "]: received key value pair (" + key.intValue() + "," + storedObject + ")");
        
        // get other replying peers
        Iterator<Entry<PeerAddress, Map<Number640, Data>>> itr = replies.iterator();
        while (itr.hasNext()) {
            System.out.println("[" + itr.next().getKey().peerId().intValue() + "]: replied for data item with key " + key.intValue());
        }
        
        return storedObject;
    }
    
    /**
     * Shuts down all peers in the provided list
     * 
     * @param peers
     */
    private static void shutDownPeers(List<PeerDHT> peers) {
        for (PeerDHT peer : peers) {
            peer.shutdown();
        }
    }
    
    /**
     * Bootstraps all given peers to the master node
     * 
     * @param peers
     */
    private static void bootstrap(List<PeerDHT> peers) {
        for (PeerDHT peer : peers) {
            peer.peerBean().peerMap().peerFound(peers.get(MASTER_NODE_ID).peerAddress(), null, null, null);
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
    private static List<PeerDHT> setupDHT(int nrOfPeers, int port) throws IOException {
        List<PeerDHT> peers = new ArrayList<>();
        
        // add master peer
        peers.add(new PeerBuilderDHT(createAndStartPeer(MASTER_NODE_ID, port)).start());
        
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
    private static Peer createAndStartPeer(int peerId, int port) throws IOException {
        PeerBuilder peerBuilder = new PeerBuilder(new Number160(peerId + 1));
        peerBuilder.ports(port);
        Peer peer = peerBuilder.start();
        
        return peer;
    }

}
