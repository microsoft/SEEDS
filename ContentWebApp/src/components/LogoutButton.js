import React from 'react';
import { getAuth, signOut } from 'firebase/auth';
import { useNavigate } from 'react-router-dom';

const LogoutButton = () => {
  const auth = getAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await signOut(auth);
      navigate('/'); // Redirect to the login page after successful logout
    } catch (error) {
      console.log('Logout error:', error);
    }
  };

  return (
    <button className="btn"
    style={{ backgroundColor: "#28574F", color: "white",  float: 'right' }} onClick={handleLogout}>
      Logout
    </button>
  );
};

export default LogoutButton;
