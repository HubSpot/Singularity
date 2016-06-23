import React from 'react';

import Header from './Header';
import MyRequests from './MyRequests';
import MyPausedRequests from './MyPausedRequests';
import MyStarredRequests from './MyStarredRequests';

const DashboardPage = () => (
  <div>
    <Header />
    <MyRequests />
    <MyPausedRequests />
    <MyStarredRequests />
  </div>
);

export default DashboardPage;
