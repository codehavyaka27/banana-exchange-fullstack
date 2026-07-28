// src/App.jsx
import { useState } from 'react';
import LandingView from './components/LandingView';
import AuthView from './components/AuthView';
import TerminalView from './components/TerminalView';

function App() {
    const [currentView, setCurrentView] = useState('LANDING');
    const [token, setToken] = useState(localStorage.getItem('token') || '');

    const handleLoginSuccess = (newToken, username) => {
        localStorage.setItem('token', newToken);
        localStorage.setItem('username', username);
        setToken(newToken);
        setCurrentView('APP');
    };

    const handleLogout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('username');
        setToken('');
        setCurrentView('LANDING');
    };

    const handleEnterExchange = () => {
        if (token) setCurrentView('APP');
        else setCurrentView('AUTH');
    };

    if (currentView === 'LANDING') {
        return <LandingView onEnter={handleEnterExchange} onLoginClick={() => setCurrentView('AUTH')} token={token} />;
    }

    if (currentView === 'AUTH') {
        return <AuthView onBack={() => setCurrentView('LANDING')} onLoginSuccess={handleLoginSuccess} />;
    }

    if (currentView === 'APP') {
        return <TerminalView token={token} onLogout={handleLogout} />;
    }

    return null;
}

export default App;