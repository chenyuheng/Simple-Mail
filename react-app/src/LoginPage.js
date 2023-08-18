import { simpleMailApiUrl } from './config.js';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import CssBaseline from '@mui/material/CssBaseline';
import TextField from '@mui/material/TextField';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Container from '@mui/material/Container';

import React, { useState } from 'react';

function LoginPage({ onLogin }) {
  const [username, setUsername] = useState('');
  const [host, setHost] = useState('pop.qq.com');
  const [port, setPort] = useState(995);
  const [password, setPassword] = useState('');

  const [usernameError, setUsernameError] = useState("");
  const [hostError, setHostError] = useState("");
  const [portError, setPortError] = useState("");
  const [passwordError, setPasswordError] = useState("");

  const [severity, setSeverity] = useState("info");
  const [alertMessage, setAlertMessage] = useState("Input your account information to log in!");

  const handleLogin = (event) => {
    event.preventDefault();
    validate();
    if (usernameError !== "" || hostError !== "" || portError !== "" || passwordError !== "") {
      return;
    }
    let postJson = {
      username: username,
      host: host,
      port: port,
      password: password
    };
    setSeverity("info");
    setAlertMessage("Logging in...");
    let url = simpleMailApiUrl + 'login';
    fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(postJson)
    })
      .then(response => {
        if (response.ok) {
          return response.json();
        } else {
          return response.text();
        }
      })
      .then(data => {
        if (typeof data === 'object') {
          setSeverity("success");
          setAlertMessage("Login successfully!");
          onLogin(data.token, data.username);
        } else {
          const startIndex = data.indexOf("-ERR");
          if (startIndex !== -1) {
            data = data.substring(startIndex); // "+4" to exclude "-ERR"
          }
          setSeverity("error");
          setAlertMessage(data);
        }
      })
      .catch(error => {
        console.error('Error:', error);
      });
  };

  const validate = () => {
    validateUsername();
    validateHost();
    validatePort();
    validatePassword();
  }

  const validateUsername = () => {
    if (username === '') {
      setUsernameError("Username cannot be empty");
    } else {
      setUsernameError("");
    }
  }

  const validateHost = () => {
    if (host === '') {
      setHostError("Host cannot be empty");
    } else {
      setHostError("");
    }
  }

  const validatePort = () => {
    if (port === '') {
      setPortError("Port cannot be empty");
    } else if (port < 0 || port > 65535) {
      setPortError("Port must be between 0 and 65535");
    } else {
      setPortError("");
    }
  }

  const validatePassword = () => {
    if (password === '') {
      setPasswordError("Password cannot be empty");
    } else {
      setPasswordError("");
    }
  }

  return (
    <Container component="main" maxWidth="xs">
      <CssBaseline />
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Alert
          severity={severity}
          sx={{ width: '100%', mb: 2 }}
        >
          {alertMessage}
        </Alert>
        <Typography component="h1" variant="h5">
          Simple Mail Client
        </Typography>
        <Box component="form" sx={{ mt: 1 }} onSubmit={handleLogin}>
          <TextField
            margin="normal"
            required
            fullWidth
            id="username"
            label="Username"
            name="username"
            autoComplete="email"
            autoFocus
            onChange={(e) => setUsername(e.target.value)}
            error={usernameError !== ""}
            helperText={usernameError}
          />
          <TextField
            margin='normal'
            required
            fullWidth
            id="host"
            label="POP3 Host"
            name="host"
            defaultValue={host}
            onChange={(e) => setHost(e.target.value)}
            error={hostError !== ""}
            helperText={hostError}
          />
          <TextField
            margin='normal'
            required
            fullWidth
            id="port"
            label="POP3 SSL/TLS Port"
            name="port"
            type="number"
            defaultValue={port}
            onChange={(e) => setPort(e.target.value)}
            error={portError !== ""}
            helperText={portError}
          />
          <TextField
            margin="normal"
            required
            fullWidth
            name="password"
            label="Password"
            type="password"
            id="password"
            autoComplete="current-password"
            onChange={(e) => setPassword(e.target.value)}
            error={passwordError !== ""}
            helperText={passwordError}
          />
          <Button
            fullWidth
            variant="contained"
            sx={{ mt: 3, mb: 2 }}
            type='submit'
          >
            Log In
          </Button>
        </Box>
      </Box>
    </Container>
  );
}

export default LoginPage;
