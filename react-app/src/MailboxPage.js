import { simpleMailApiUrl } from './config.js';

import React, { useState, useEffect } from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Grid from '@mui/material/Grid';
import Paper from '@mui/material/Paper';
import AppBar from '@mui/material/AppBar';
import CssBaseline from '@mui/material/CssBaseline';
import Toolbar from '@mui/material/Toolbar';
import List from '@mui/material/List';
import Typography from '@mui/material/Typography';
import Divider from '@mui/material/Divider';
import ListItem from '@mui/material/ListItem';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemText from '@mui/material/ListItemText';
import Pagination from '@mui/material/Pagination';

const previewLength = 50;

function MailboxPage({ token, username, onLogout }) {
  const [mailListDisplay, setMailListDisplay] = useState(JSON.parse(localStorage.getItem("mailListDisplay")) || [{ id: 1, subject: 'Loading mails...' }]);
  const [currentMail, setCurrentMail] = useState({ id: 1, subject: 'Please select a mail from the list to view' });
  const [mailSelected, setMailSelected] = useState(false);
  const [pageCount, setPageCount] = useState(Math.ceil((localStorage.getItem('totalMailCount') || 1) / 10));

  const fetchMail = (mailId) => {
    console.debug('fetching mail ' + mailId);
    const url = simpleMailApiUrl + 'mails/' + mailId;
    fetch(url, {
      method: 'GET',
      headers: {
        "Content-Type": "application/json",
        "Authorization": token,
      },
    }).then(response => {
      if (response.ok) {
        return response.json();
      } else {
        return response.text();
      }
    }).then(data => {
      if (typeof data === 'object') {
        console.debug("set mail id in local storage: " + mailId);
        localStorage.setItem('mail/' + mailId, JSON.stringify(data));
        updateMailListDisplay();
        setMailSelected(true);
      } else {
        console.error(data);
      }
    }).catch(error => {
      console.error(error);
    });
  };

  const updateMailListDisplay = () => {
    let totalMailCount = localStorage.getItem('totalMailCount');
    let currentPage = localStorage.getItem('currentPage') || 1;
    let mailList = [];
    let start = totalMailCount - (currentPage - 1) * 10;
    let end = totalMailCount - (currentPage - 1) * 10 - 9;
    if (end < 1) {
      end = 1;
    }
    for (let i = start; i >= end; i--) {
      let mailContent = localStorage.getItem('mail/' + i);
      if (mailContent === null) {
        fetchMail(i);
        continue;
      }
      let mail = JSON.parse(mailContent);
      if (mail === null) {
        continue;
      }
      mail.content = new DOMParser().parseFromString(mail.content, 'text/html').body.textContent || "";
      if (mail.content.length > previewLength) {
        mail.content = mail.content.substring(0, previewLength) + '...';
      }
      mailList.push(mail);
    }
    if (currentPage === 1) {
      localStorage.setItem("mailListDisplay", JSON.stringify(mailList));
    }
    setMailListDisplay(mailList);
  };

  useEffect(() => {
    const url = simpleMailApiUrl + 'mails_count';
    fetch(url, {
      method: 'GET',
      headers: {
        "Content-Type": "application/json",
        "Authorization": token,
      },
    }).then(response => {
      if (response.ok) {
        return response.json();
      } else {
        return response.text();
      }
    }).then(data => {
      if (typeof data === 'number') {
        localStorage.setItem('totalMailCount', data);
        setPageCount(Math.ceil(data / 10));
      } else {
        console.error(data);
      }
      updateMailListDisplay();
    }).catch(error => {
      console.error(error);
    });
  }, []);

  const handleListItemClick = (mailId) => {
    console.log('mail #' + mailId + ' clicked');
    const mailContent = localStorage.getItem('mail/' + mailId);
    if (mailContent === null) {
      return;
    }
    const mail = JSON.parse(mailContent);
    setCurrentMail(mail);
    setMailSelected(true);
  };

  const handlePageChange = (event, value) => {
    localStorage.setItem('currentPage', value);
    updateMailListDisplay();
  };

  const renderMailList = () => {
    let helperText = '';
    if (mailListDisplay.length === 0) {
      helperText = 'Loading mail list...';
    }
    if (localStorage.getItem('totalMailCount') === '0') {
      helperText = 'No mail found';
    }
    if (helperText !== '') {
      return (
        <ListItem>
          <ListItemText primary="Loading mail list..." />
        </ListItem>
      );
    }
    return mailListDisplay.map((item, index) => (
      <React.Fragment key={item.id}>
        {index !== 0 && <Divider component="li" />}
        <ListItemButton key={item.id} onClick={() => handleListItemClick(item.id)}>
          <ListItemText
            primary={item.subject}
            secondary={
              <React.Fragment>
                <Typography
                  sx={{ display: 'inline' }}
                  component="span"
                  variant="body2"
                  color="text.primary"
                >
                  {item.from}
                </Typography>
                <br />
                {item.content}
              </React.Fragment>
            }
          />
        </ListItemButton>
      </React.Fragment>
    ));
  };

  return (
    <div>
      <CssBaseline />
      <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Simple Mail Client
          </Typography>
          <Button color="inherit" sx={{ textTransform: 'none' }} onClick={onLogout}>
            Logout: {username}
          </Button>
        </Toolbar>
      </AppBar>
      <div style={{ marginTop: '78px' }}>
        <Container component="main" maxWidth="lg">
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <Paper elevation={3} style={{ padding: '16px', height: "87vh" }}>
                <Box style={{ height: '80vh', overflow: 'auto', marginBottom: "1vh" }}>
                  <List>
                    {renderMailList()}
                  </List>
                </Box>
                <Grid container justifyContent="center">

                  <Pagination
                    count={pageCount}
                    color="primary"
                    size="small"
                    variant="outlined"
                    shape="rounded"
                    onChange={handlePageChange}
                  />
                </Grid>
              </Paper>
            </Grid>

            <Grid item xs={12} md={8}>
              <Paper elevation={3} style={{ padding: '16px', height: "87vh" }}>
                <Box style={{ maxHeight: '80vh', overflow: 'auto' }}>
                  <Typography variant="h5">{currentMail.subject}</Typography>
                  <Typography variant="subtitle2" hidden={!mailSelected}>From: {currentMail.from}</Typography>
                  <Typography variant="subtitle2" hidden={!mailSelected}>To: {currentMail.to}</Typography>
                  <Typography variant="subtitle2" hidden={!mailSelected}>Date: {currentMail.date}</Typography>
                  <div dangerouslySetInnerHTML={{ __html: currentMail.content }} />
                </Box>
              </Paper>
            </Grid>

          </Grid>
        </Container>
      </div>
    </div>
  );
}

export default MailboxPage;
