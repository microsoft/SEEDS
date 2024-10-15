import React from 'react';
import ReactDOM from 'react-dom/client'; // Use the new ReactDOM API from 'react-dom/client'
import App from './App';
import { ConferenceProvider } from './context/ConferenceContext'; // Import ConferenceProvider
import './index.css';

// Find the root element in your HTML
const rootElement = document.getElementById('root');

// Create a root and render the app using the new API
const root = ReactDOM.createRoot(rootElement);
root.render(
  <React.StrictMode>
    <ConferenceProvider>
      <App />
    </ConferenceProvider>
  </React.StrictMode>
);
