import React from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import Header from './Header';
import MyRequests from './MyRequests';
import MyGroupRequests from './MyGroupRequests';
import MyPausedRequests from './MyPausedRequests';
import MyStarredRequests from './MyStarredRequests';

import { FetchRequests } from '../../actions/api/requests';

const DashboardPage = () => (
  <div>
    <Header />
    <MyRequests />
    <MyPausedRequests />
    <MyStarredRequests />
    <MyGroupRequests />
  </div>
);

function mapDispatchToProps(dispatch) {
  return {
    fetchRequests: () => dispatch(FetchRequests.trigger())
  };
}

function refresh(props) {
  return props.fetchRequests();
}

export default connect(null, mapDispatchToProps)(rootComponent(DashboardPage, 'Dashboard', refresh));
