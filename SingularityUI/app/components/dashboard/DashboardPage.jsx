import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { Row, Col } from 'react-bootstrap';

import Utils from '../../utils';

import * as StarredActions from '../../actions/ui/starred';
import * as RequestActions from '../../actions/api/request';
import * as RequestsSelectors from '../../selectors/requests';

import UITable from '../common/table/UITable';
import { Starred, RequestId, Type, LastDeploy, DeployUser, State, Actions } from '../requests/Columns';

import RequestCounts from './RequestCounts';
import RequestCount from './RequestCount';

class DashboardPage extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'DashboardPage';
  }

  renderHeader() {
    let headerData = <h1>Singularity</h1>;
    const deployUser = Utils.maybe(this.props.userAPI.data, [
      'user',
      'id'
    ]);
    if (deployUser) {
      headerData = <h1>{deployUser}</h1>;
    }

    return (
      <header>
        {headerData}
      </header>
    );
  }

  renderMyRequests() {
    const totals = this.props.userRequestTotals;
    const deployUser = Utils.maybe(this.props.userAPI.data, [
      'user',
      'id'
    ]);

    const myRequests = (
      <RequestCounts>
        <RequestCount
          label={'total'}
          count={totals.total}
          link={`${config.appRoot}/requests/active/all/${deployUser}`}
        />
        <RequestCount
          label={'on demand'}
          count={totals.ON_DEMAND}
          link={`${config.appRoot}/requests/active/ON_DEMAND/${deployUser}`}
        />
        <RequestCount
          label={'worker'}
          count={totals.WORKER}
          link={`${config.appRoot}/requests/active/WORKER/${deployUser}`}
        />
        <RequestCount
          label={'scheduled'}
          count={totals.SCHEDULED}
          link={`${config.appRoot}/requests/active/SCHEDULED/${deployUser}`}
        />
        <RequestCount
          label={'run once'}
          count={totals.RUN_ONCE}
          link={`${config.appRoot}/requests/active/RUN_ONCE/${deployUser}`}
        />
        <RequestCount
          label={'service'}
          count={totals.SERVICE}
          link={`${config.appRoot}/requests/active/SERVICE/${deployUser}`}
        />
      </RequestCounts>
    );

    return myRequests;
  }

  renderMyPausedRequests() {
    let pausedRequestsSection = (
      <div className='empty-table-message'><p>No paused requests</p></div>
    );

    const pausedRequests = this.props.userRequests.filter((r) => r.state === 'PAUSED');
    const starredRequests = this.props.requests.filter((r) => 'starred' in r && r.starred);

    if (pausedRequests.length > 0) {
      pausedRequestsSection = (
        <UITable
          data={pausedRequests}
          keyGetter={(r) => r.request.id}
          asyncSort
          paginated
          rowChunkSize={10}
        >
          {RequestId}
          {Type}
          {LastDeploy}
          {Actions(this.props.unpauseAction, this.props.removeAction)}
        </UITable>
      );
    }

    return (
      <Row>
          <Col md={12} className='table-staged'>
              <div className='page-header'>
                  <h2>My paused requests</h2>
              </div>
              {pausedRequestsSection}
          </Col>
      </Row>
    );
  }

  renderStarredRequests() {
    let starredRequestsSection = (
      <div className='empty-table-message'><p>No starred requests</p></div>
    );
    const starredRequests = this.props.requests.filter((r) => 'starred' in r && r.starred);

    if (starredRequests.length > 0) {
      starredRequestsSection = (
        <UITable
          data={starredRequests}
          keyGetter={(r) => r.request.id}
          asyncSort
          paginated
          rowChunkSize={10}
        >
          {Starred(this.props.changeStar)}
          {RequestId}
          {Type}
          {LastDeploy}
          {DeployUser}
          {State}
        </UITable>
      );
    }

    return (
      <Row>
          <Col md={12} className='table-staged'>
              <div className='page-header'>
                  <h2>Starred requests</h2>
              </div>
              {starredRequestsSection}
          </Col>
      </Row>
    );
  }

  render() {
    return (
      <div>
        {this.renderHeader()}
        {this.renderMyRequests()}
        {this.renderMyPausedRequests()}
        {this.renderStarredRequests()}
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  const requestsAPI = state.api.requests;
  const userAPI = state.api.user;
  return {
    requests: RequestsSelectors.combineStarredWithRequests(state),
    userRequests: RequestsSelectors.getUserRequests(state),
    userRequestTotals: RequestsSelectors.getUserRequestTotals(state),
    userAPI
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    changeStar: (requestId) => {
      dispatch(StarredActions.changeRequestStar(requestId));
    },
    unpauseAction: (requestId, message) => {
      dispatch(RequestActions.UnpauseAction.trigger(requestId, message));
    },
    removeAction: (requestId, message) => {
      dispatch(RequestActions.RemoveAction.trigger(requestId, message));
    }
  };
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DashboardPage);
