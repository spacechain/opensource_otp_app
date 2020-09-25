package com.google.android.apps.authenticator.spc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPrivateKeySpec;


import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.security.MessageDigest;

import android.util.Base64;

public class RSAEncrypt{
    private RSAPrivateKey privateKey;

    private RSAPublicKey publicKey;

    public String getPrivateKey() {
        return Base64.encodeToString(privateKey.getEncoded(), android.util.Base64.DEFAULT);
    }

    public RSAPrivateKey getPrivateKey2() {
        return privateKey;
    }

    public String getPublicKey() {
        return Base64.encodeToString(publicKey.getEncoded(), android.util.Base64.DEFAULT);

    }

    public RSAPublicKey getPublicKey2() {
        return publicKey;
    }

    public void genKeyPair() {
        KeyPairGenerator keyPairGen = null;
        try {
            keyPairGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyPairGen.initialize(1024, new SecureRandom());
        KeyPair keyPair = keyPairGen.generateKeyPair();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
    }

    public void loadPublicKey(String publicKeyStr) {
        try {
            byte[] buffer = Base64.decode(publicKeyStr, android.util.Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            this.publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
        } catch (InvalidKeySpecException e) {
        } catch (NullPointerException e) {
        }
    }

    public void loadPrivateKey(String privateKeyStr) throws Exception {
        try {
            byte[] buffer = Base64.decode(privateKeyStr, android.util.Base64.DEFAULT);

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);


        } catch (NoSuchAlgorithmException e) {
        } catch (InvalidKeySpecException e) {
        } catch (NullPointerException e) {
        }
    }

    public byte[] encrypt(RSAPublicKey publicKey, byte[] plainTextData) throws Exception {
        if (publicKey == null) {
            throw new Exception("Can't be empty");
        }
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] output = cipher.doFinal(plainTextData);
            return output;
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("There is no such encryption algorithm");
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeyException e) {
            throw new Exception("The encrypted public key is illegal, please check");
        } catch (IllegalBlockSizeException e) {
            throw new Exception("Illegal plaintext length");
        } catch (BadPaddingException e) {
            throw new Exception("The plaintext data is corrupted");
        }
    }


    public byte[] decrypt(RSAPrivateKey privateKey, byte[] cipherData) throws Exception {
        if (privateKey == null) {
            //      throw new Exception("解密私钥为空, 请设置");
            return "Can't be empty".getBytes();
        }
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] output = cipher.doFinal(cipherData);
            return output;
        } catch (NoSuchAlgorithmException e) {
            //      throw new Exception("无此解密算法");
            return "There is no such decryption algorithm".getBytes();

        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return "22a2".getBytes();

        } catch (InvalidKeyException e) {
            //      throw new Exception("解密私钥非法,请检查");
            return "Decrypting the private key is illegal, please check".getBytes();

        } catch (IllegalBlockSizeException e) {
            //      throw new Exception("密文长度非法");
            return "The length of ciphertext is illegal".getBytes();

        } catch (BadPaddingException e) {
            //      throw new Exception("密文数据已损坏");
            return "Ciphertext data is corrupted".getBytes();

        }
    }
}