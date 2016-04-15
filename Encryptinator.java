/**
 * Handles all of the encryption/decrytpion of the program and manages the encryption keys
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;



public class Encryptinator {

	public static final String RSAKeyPrivate = "private.der", RSAKeyPublic = "public.der", AESKey = "AESKey.key"; 
	public static final int	AES_KEY_SIZE = 128, BUFFER_SIZE = 1024;
	//gives indication as to progess of current encryption/decryption
	private float PROGRESS; 
	byte[] aesKeyBytes;
	Cipher aesCipher, pkCipher;
	SecretKeySpec aesKeySpec;
	
	public Encryptinator(){
		//when progress not being used, set to -1.0
		PROGRESS = -1.0f;	
			
		try {
			aesCipher = Cipher.getInstance("AES");
			pkCipher = Cipher.getInstance("RSA");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) { e.printStackTrace(); }
	
		//check if an AES Key exists. If it does, decrypt it and save in aesKeySpec. If not, create one, encrypt it,
		//and save it to disk for future use
		File RSAPrivate = new File(RSAKeyPrivate);
		File RSAPublic = new File(RSAKeyPublic);
		File AESKeyFile = new File(AESKey);
		if(!RSAPrivate.exists() || !RSAPublic.exists()){
			System.out.println("ERROR: Encryptinator couldn't find the RSA keys...'tis useless captain!");
		} else {
			if(!AESKeyFile.exists()){
				System.out.println("WARNING: Encryptinator couldnt find AES key, so went ahead and made one!");
				generateKey();
				encryptAESKey(AESKeyFile, RSAPublic);
			} else {
				decryptAESKey(AESKeyFile, RSAPrivate);
			}
		}
	}

	/**
	 * Creates a file on disk if not already present
	 * @param file
	 */
	private void createFileIfAbsent(File file){
		System.out.println("CFIA " + file.getPath());
		try {
			if(!file.exists()){
				file.createNewFile();
			}
		} catch (IOException e) { e.printStackTrace(); }
	}

	/**
	 * Encrypts file and saves encrypted file to disk
	 * @param toEncrypt File to encrypt
	 * @param encryptedFile File to save encrypted file
	 */
	public void encrypt(File toEncrypt, File encryptedFile){
		createFileIfAbsent(toEncrypt);
		createFileIfAbsent(encryptedFile);
		try {
			aesCipher.init(Cipher.ENCRYPT_MODE, aesKeySpec);
			FileInputStream is = new FileInputStream(toEncrypt);
			CipherOutputStream os = new CipherOutputStream(new FileOutputStream(encryptedFile), aesCipher);
			PROGRESS = 0.0f;
			float fileSize = toEncrypt.length(), readBytes = 0.0f;
			int i;
			byte[] buffer = new byte[BUFFER_SIZE];
			while((i = is.read(buffer)) != -1){
				os.write(buffer, 0, i);
				readBytes += (float)i;
				PROGRESS = readBytes / fileSize;
				System.out.println(PROGRESS);
			}
			
			is.close();
			os.close();
			
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	/**
	 * Decrypts file and saves encrypted file to disk
	 * @param toDecrypt File file to decrypt
	 * @param decrypted File to decrypt
	 */
	public void decrypt(File toDecrypt, File decrypted){
		createFileIfAbsent(toDecrypt);
		createFileIfAbsent(decrypted);
		try {
			aesCipher.init(Cipher.DECRYPT_MODE, aesKeySpec);
			CipherInputStream is = new CipherInputStream(new FileInputStream(toDecrypt), aesCipher);
			FileOutputStream os = new FileOutputStream(decrypted);
			PROGRESS = 0.0f;
			float fileSize = toDecrypt.length(), readBytes = 0.0f;
			int i;
			byte[] buffer = new byte[BUFFER_SIZE];
			while((i = is.read(buffer)) != -1){
				os.write(buffer, 0, i);
				readBytes += (float)i;
				PROGRESS = readBytes / fileSize;
				System.out.println(PROGRESS);
			}
			
			is.close();
			os.close();
			
		} catch (Exception e) { e.printStackTrace(); }
		
	}
	
	/**
	 * Generates a new AES key
	 */
	private void generateKey(){
		try {
			KeyGenerator kGen = KeyGenerator.getInstance("AES");
			kGen.init(AES_KEY_SIZE);
			SecretKey aesKey = kGen.generateKey();
			aesKeyBytes = aesKey.getEncoded();
			aesKeySpec = new SecretKeySpec(aesKeyBytes, "AES");
		} catch (NoSuchAlgorithmException e) { e.printStackTrace(); }
		
	}
	
	/**
	 * Reads in the RSA key and uses it to decrypt the AES key that is used to encrypt/decrypt files
	 * sets the global aesKeySpec
	 * @param encryptedAES
	 * @param privateRSAKey
	 */
	private void decryptAESKey(File encryptedAES, File privateRSAKey){
		try {
			byte[] encodedKey = new byte[(int)privateRSAKey.length()];
			FileInputStream privateIS = new FileInputStream(privateRSAKey);
			privateIS.read(encodedKey);
			privateIS.close();
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedKey);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			PrivateKey pk = kf.generatePrivate(privateKeySpec);
			
			pkCipher.init(Cipher.DECRYPT_MODE, pk);
			aesKeyBytes = new byte[AES_KEY_SIZE/8];
			CipherInputStream is = new CipherInputStream(new FileInputStream(encryptedAES), pkCipher);
			is.read(aesKeyBytes);
			aesKeySpec = new SecretKeySpec(aesKeyBytes, "AES");
			is.close();
		} catch (Exception e) { e.printStackTrace(); }
		
	}
	
	/**
	 * Reads the RSA key and uses it to encrypt the generated AES key bytes (aesKeyBytes)and save it to disk
	 * to be used later
	 * @param out
	 * @param publicRSAKey
	 */
	private void encryptAESKey(File out, File publicRSAKey){
		try {
			byte[] encodedKey = new byte[(int)publicRSAKey.length()];
			FileInputStream publicIS = new FileInputStream(publicRSAKey);
			publicIS.read(encodedKey);
			publicIS.close();
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedKey);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			PublicKey pk = kf.generatePublic(publicKeySpec);
			 
			pkCipher.init(Cipher.ENCRYPT_MODE, pk);
			CipherOutputStream os = new CipherOutputStream(new FileOutputStream(out), pkCipher);
			os.write(aesKeyBytes);
			os.close();
			
		} catch (Exception e) { e.printStackTrace(); }
	}
}
