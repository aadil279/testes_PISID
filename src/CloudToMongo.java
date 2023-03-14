import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.mongodb.*;
import com.mongodb.util.JSON;

import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CloudToMongo  implements MqttCallback {
    MqttClient mqttclient;
    static MongoClient mongoClient;
    static DB db;
    static DBCollection mongocol_temp;
    static DBCollection mongocol_sala;
    static String mongo_user = new String();
    static String mongo_password = new String();
    static String mongo_address = new String();
    static String cloud_server = new String();
    static String cloud_topic_temp = new String();
    static String cloud_topic_sala = new String();
    static String mongo_host = new String();
    static String mongo_replica = new String();
    static String mongo_database = new String();
    static String mongo_collection_sala = new String();
    static String mongo_collection_temp = new String();
    static String mongo_authentication = new String();
    static JTextArea documentLabel = new JTextArea("\n");


    private static void createWindow() {
        JFrame frame = new JFrame("Cloud to Mongo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel textLabel = new JLabel("Data from broker: ",SwingConstants.CENTER);
        textLabel.setPreferredSize(new Dimension(600, 30));
        JScrollPane scroll = new JScrollPane (documentLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setPreferredSize(new Dimension(600, 200));
        JButton b1 = new JButton("Stop the program");
        frame.getContentPane().add(textLabel, BorderLayout.PAGE_START);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.getContentPane().add(b1, BorderLayout.PAGE_END);
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);
        b1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                System.exit(0);
            }
        });
    }

    public static void main(String[] args) {
        createWindow();
        try {
            Properties p = new Properties();
            p.load(new FileInputStream("cloudToMongo.ini"));
            mongo_address = p.getProperty("mongo_address");
            mongo_user = p.getProperty("mongo_user");
            mongo_password = p.getProperty("mongo_password");
            mongo_replica = p.getProperty("mongo_replica");
            cloud_server = p.getProperty("cloud_server");
            cloud_topic_temp = p.getProperty("cloud_topic_temp");
            cloud_topic_sala = p.getProperty("cloud_topic_sala");
            mongo_host = p.getProperty("mongo_host");
            mongo_database = p.getProperty("mongo_database");
            mongo_authentication = p.getProperty("mongo_authentication");
            mongo_collection_sala = p.getProperty("mongo_collection_sala");
            mongo_collection_temp = p.getProperty("mongo_collection_temp");
        } catch (Exception e) {
            System.out.println("Error reading CloudToMongo.ini file " + e);
            JOptionPane.showMessageDialog(null, "The CloudToMongo.inifile wasn't found.", "CloudToMongo", JOptionPane.ERROR_MESSAGE);
        }
        new CloudToMongo().connectCloud();
        new CloudToMongo().connectMongo();
    }

    public void connectCloud() {
        int i;
        try {
            i = new Random().nextInt(100000);
            mqttclient = new MqttClient(cloud_server, "CloudToMongo_"+String.valueOf(i)+"_"+ cloud_topic_temp);
            mqttclient.connect();
            mqttclient.setCallback(this);
            mqttclient.subscribe(cloud_topic_temp);
            mqttclient.subscribe(cloud_topic_sala);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void connectMongo() {
        String mongoURI = new String();
        mongoURI = "mongodb://";
        if (mongo_authentication.equals("true")) mongoURI = mongoURI + mongo_user + ":" + mongo_password + "@";
        mongoURI = mongoURI + mongo_address;
        if (!mongo_replica.equals("false"))
            if (mongo_authentication.equals("true")) mongoURI = mongoURI + "/?replicaSet=" + mongo_replica+"&authSource=admin";
            else mongoURI = mongoURI + "/?replicaSet=" + mongo_replica;
        else
        if (mongo_authentication.equals("true")) mongoURI = mongoURI  + "/?authSource=admin";
        MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoURI));
        db = mongoClient.getDB(mongo_database);
        mongocol_temp = db.getCollection(mongo_collection_temp);
        mongocol_sala = db.getCollection(mongo_collection_sala);
    }

    @Override
    public void messageArrived(String topic, MqttMessage c)
            throws Exception {
        try {
            if(c == null) System.out.println("c is null");

            DBObject document_json;
            document_json = (DBObject) JSON.parse(c.toString());
            documentLabel.append(c.toString()+"\n");

            if(topic.equals(cloud_topic_sala) && mongocol_sala != null) {
                mongocol_sala.insert(document_json);
            }
            else if(topic.equals(cloud_topic_temp) && mongocol_temp != null)  {
                mongocol_temp.insert(document_json);
            }

            if(mongocol_sala == null || mongocol_temp == null) {
                System.out.println("connecting to collections...");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}
