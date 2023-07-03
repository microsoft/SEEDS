import React from 'react';
import firebase from 'firebase/app';
import 'firebase/auth';
import { initializeApp } from "firebase/app";
import firebaseConfig from '../firebase';
import { getAuth, signInWithPopup, GoogleAuthProvider } from "firebase/auth";
import { useNavigate } from 'react-router-dom';
import { useState } from 'react';


const Login = () => {
    const auth = getAuth();
    const navigate = useNavigate();
    const [showError, setShowError] = useState(false);
    // console.log()

    const handleGoogleSignIn = () => {
      const provider = new GoogleAuthProvider();
  
      signInWithPopup(auth, provider)
        .then((result) => {
          // This gives you a Google Access Token. You can use it to access the Google API.
          const credential = GoogleAuthProvider.credentialFromResult(result);
          const token = credential.accessToken;
          // The signed-in user info.
          const user = result.user;
          console.log(user.displayName)
          // IdP data available using getAdditionalUserInfo(result)
            navigate('/content', {state: {name:user.displayName}});

          // ...
        })
        .catch((error) => {
          // Handle Errors here.
          const errorCode = error.code;
          const errorMessage = error.message;
          // The email of the user's account used.
          const email = error.customData.email;
          // The AuthCredential type that was used.
          const credential = GoogleAuthProvider.credentialFromError(error);
          showError(true);
          // ...
        });
    };
  
    return (
      <div style={{ display: 'flex',  flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '100vh'}}>
        <h1>Welcome to SEEDS</h1>
        <br/>
        <button className="btn" style={{ backgroundColor: "#28574F", color: "white"}} onClick={handleGoogleSignIn}>Sign in with Google</button>
        {showError && <p>Error Occured</p>}
      </div>
    );
  };
  

export default Login;