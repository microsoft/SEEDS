import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries
// Your web app's Firebase configuration

const firebaseConfig = {
  apiKey: process.env.REACT_APP_FIREBASEAPIKEY,
  authDomain: process.env.REACT_APP_FIREBASEAUTHDOMAIN,
  projectId: process.env.REACT_APP_FIREBASEPROJECTID,
  storageBucket: process.env.REACT_APP_FIREBASESTORAGEBUCKET,
  messagingSenderId: process.env.REACT_APP_FIREBASEMESSAGINGSENDERID,
  appId: process.env.REACT_APP_FIREBASEAPPID,
  measurementId: process.env.REACT_APP_FIREBASEMEASUREMENTID
};

export default firebaseConfig;
