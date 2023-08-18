import React, { useState } from 'react';
import LoginPage from './LoginPage';
import MailboxPage from './MailboxPage';

function App() {
  const [token, setToken] = useState(localStorage.getItem('token') || null);
  const [username, setUsername] = useState(localStorage.getItem('username') || '');

  const handleLogin = (newToken, username) => {
    setToken(newToken);
    setUsername(username);
    localStorage.setItem('token', newToken);
    localStorage.setItem('username', username);
  };

  const handleLogout = () => {
    setToken(null);
    setUsername('');
    localStorage.clear();
  };

  return (
    <div className="App">
      {token ? (
        <MailboxPage token={token} username={username} onLogout={handleLogout} />
      ) : (
        <LoginPage onLogin={handleLogin} />
      )}
    </div>
  );
}

export default App;
