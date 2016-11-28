import React from 'react';
import rootComponent from '../../rootComponent';

import Header from './Header';
import MyRequests from './MyRequests';
import MyPausedRequests from './MyPausedRequests';
import MyStarredRequests from './MyStarredRequests';

import { refresh } from '../../actions/ui/dashboard';

const DashboardPage = () => (
  <div>
    <Header />
    <MyRequests />
    <MyPausedRequests />
    <MyStarredRequests />
  </div>
);

export default rootComponent(DashboardPage, refresh);
