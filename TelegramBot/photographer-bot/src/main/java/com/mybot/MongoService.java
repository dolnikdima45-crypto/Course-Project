package com.mybot;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import com.mongodb.client.result.DeleteResult; 

import java.util.ArrayList;
import java.util.List;

public class MongoService {

    private final MongoCollection<Document> ac;

    public MongoService(String cs, String dn) {
        MongoClient mc = MongoClients.create(cs);
        MongoDatabase db = mc.getDatabase(dn);
        this.ac = db.getCollection("appointments");
    }

    public boolean isa(String d, String t) {
        Document q = new Document("date", d).append("time", t);
        return ac.find(q).first() == null;
    }

    public boolean ca(long uid, String un, String d, String t, String s) {
        if (!isa(d, t)) {
            return false; 
        }

        Document na = new Document("user_id", uid)
                .append("user_name", un)
                .append("date", d)
                .append("time", t)
                .append("service_type", s)
                .append("status", "confirmed");

        ac.insertOne(na);
        return true;
    }

    public List<Document> gua(long uid) {
        return ac.find(Filters.eq("user_id", uid)).into(new ArrayList<>());
    }
    
    public boolean da(long uid, String d, String t) {
        Document q = new Document("user_id", uid)
                .append("date", d)
                .append("time", t);

        DeleteResult r = ac.deleteOne(q);
        
        return r.getDeletedCount() > 0;
    }
}