"use strict";
const mongoose = require('mongoose');
const crypto = require('crypto');

const UserInfoSchema = new mongoose.Schema({
  _id: {
    type: String,
    required: true
  },
  name: {
    type: String,
    required: true
  },
  encrypted_phone_number: {
    type: String,
    required: true
  },
  encryption_iv: {
    type: String,
    required: true
  },
  encryption_salt: {
    type: String,
    required: true
  },
  role: {
    type: String,
    required: true
  },
  created_at: { 
    type: Number, 
    default: -1 
  },
  organisation: {
    type: String,
    required: false,
    default: ""
  }
}, { collection: 'userInfo' });

const UserInfo = mongoose.model("UserInfo", UserInfoSchema);

function decryptMessage(encryptedText, password, iv, salt) {
    const keyLength = 32; // Corresponds to 256 bits for AES-256
    const iterations = 100000;
    const digest = 'sha256';

    const saltBuffer = Buffer.from(salt, 'base64');
    const ivBuffer = Buffer.from(iv, 'base64');
    const encryptedData = Buffer.from(encryptedText, 'base64');

    const key = crypto.pbkdf2Sync(password, saltBuffer, iterations, keyLength, digest);
    const decipher = crypto.createDecipheriv('aes-256-cbc', key, ivBuffer);
    let decrypted = decipher.update(encryptedData, null, 'utf8');
    decrypted += decipher.final('utf8');
    return decrypted;
}

module.exports.getAllUsers = async (encryptionPassword) => {
    try {
        const users = await UserInfo.find().sort({created_at: -1}).exec();
        const decryptedUsers = users.map(user => {
            const phoneNumber = decryptMessage(user.encrypted_phone_number, encryptionPassword, user.encryption_iv, user.encryption_salt);
            return {
                ...user.toObject(),
                phoneNumber,
                _id: undefined, // Remove userID from the response
                encrypted_phone_number: undefined, // Remove the encrypted phone number from the response
                encryption_iv: undefined, // Optional: remove IV from the response
                encryption_salt: undefined // Optional: remove Salt from the response
            };
        });
        return decryptedUsers;
    } catch (error) {
        console.error("Failed to get and decrypt users:", error);
        throw error;
    }
};
