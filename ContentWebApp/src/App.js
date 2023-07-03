
import AddQuiz from './components/AddQuiz';
import AllContent from './components/AllContent';
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { useNavigate } from 'react-router-dom';
import { getAuth, onAuthStateChanged } from 'firebase/auth';
import ContentDetails from "./components/ContentDetails";
import ContentEdit from "./components/ContentEdit";
import AddContent from './components/AddContent';
import IVR from './components/IVR';
import './App.css'
import Login from './components/Login';
import { useState, useEffect } from 'react';

const ProtectedRoute = ({ path, ...props }) => {
  const auth = getAuth();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setUser(user);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [auth]);

  if (loading) {
    return <div>Loading...</div>; // Or show a loading spinner
  }

  if (!user) {
    navigate('/login');
    return null;
  }

  return <Route path={path} {...props} />;
};

function App() {
  
  return (
    <div className="App">
      <BrowserRouter>
        <Routes>
          <Route path='/' element={<Login />}></Route>
          <Route path='/content' element={<AllContent />}/>
          <Route path='/content/create' element={<AddContent />}/>
          <Route path='/content/detail/:type/:id' element={<ContentDetails />}/>
          <Route path='/content/edit/:type/:id' element={<ContentEdit />}/>
          <Route path ='/ivr' element={<IVR />}/>
        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;
