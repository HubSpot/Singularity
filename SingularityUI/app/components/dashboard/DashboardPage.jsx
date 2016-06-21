import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import { Row, Col } from 'react-bootstrap';

import * as StarredActions from '../../actions/ui/starred';
import { combineStarredWithRequests } from '../../selectors/requests';

import UITable from '../common/table/UITable';
import { Starred, RequestId, Type, LastDeploy, DeployUser, State } from '../requests/Columns';

import RequestCounts from './RequestCounts';
import RequestCount from './RequestCount';

class DashboardPage extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'DashboardPage';
  }

  renderHeader() {
    let headerData = <h1>Singularity</h1>;
    if (this.props.user.user.id) {
      headerData = <h1>{this.props.user.user.id}</h1>;
    }

    return (
      <header>
        {headerData}
      </header>
    );
  }

  renderMyRequests() {


    const myRequests = (
      <RequestCounts>
        <RequestCount
          label={'total'}
          count={0}
        />
        <RequestCount
          label={'on demand'}
          count={0}
        />
        <RequestCount
          label={'worker'}
          count={0}
        />
        <RequestCount
          label={'scheduled'}
          count={0}
        />
        <RequestCount
          label={'run once'}
          count={0}
        />
        <RequestCount
          label={'service'}
          count={0}
        />
      </RequestCounts>
    );

    return myRequests;
  }

  renderMyPausedRequests() {
    let myPausedRequests = (
      <div class="empty-table-message"><p>No starred requests</p></div>
    );
    if (this.props.requests.length > 0) {
      myPausedRequests = (
        <UITable
          data={this.props.requests}
          keyGetter={(r) => r.request.id}
          asyncSort
          paginated
          rowChunkSize={10}
        >
          {RequestId}
          {Type}
          {LastDeploy}
          {DeployUser}
          {State}
        </UITable>
      );
    }

    return myPausedRequests;
  }

  renderStarredRequests() {
    let starredRequestsSection = (
      <div class="empty-table-message"><p>No starred requests</p></div>
    );
    const starredRequests = this.props.requests.filter((r) => 'starred' in r);

    if (starredRequests.length > 0) {
      const starredRequestsTable = (
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

      starredRequestsSection = (
        <Row>
            <Col md={12} className='table-staged'>
                <div class='page-header'>
                    <h2>Starred requests</h2>
                </div>
                {starredRequestsTable}
            </Col>
        </Row>
      );
    }

    return starredRequestsSection;
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
    requests: combineStarredWithRequests(state),
    userAPI
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    changeStar: (requestId) => {
      dispatch(StarredActions.changeRequestStar(requestId))
    }
  };
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DashboardPage);
