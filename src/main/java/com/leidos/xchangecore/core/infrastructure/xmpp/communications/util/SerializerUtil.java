package com.leidos.xchangecore.core.infrastructure.xmpp.communications.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.xerces.impl.dv.util.Base64;

public class SerializerUtil {

    public static Object deserialize(String s) {

        byte[] bytes = Base64.decode(s);
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object o = ois.readObject();
            ois.close();
            return o;
        } catch (Exception e) {
            return null;
        }
    }

    public static String serialize(Serializable o) {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.close();
            return new String(Base64.encode(baos.toByteArray()));
        } catch (Exception e) {
            return null;
        }
    }
}
