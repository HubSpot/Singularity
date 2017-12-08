import React, { Component, PropTypes } from 'react';
import { Row, Col } from 'react-bootstrap';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { Link } from 'react-router';

import Utils from '../../utils';
import Loader from '../common/Loader';

import { refresh } from '../../actions/ui/dashboard';
import { FetchUserRelevantRequests } from '../../actions/api/requests';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import Section from '../common/Section';
import RequestTypeIcon from '../common/icons/RequestTypeIcon'
import * as Cols from '../requests/Columns';
import RequestSummaryBox from './RequestSummaryBox';
import RequestFilters from '../requests/RequestFilters';

class DashboardPage extends Component {
  static propTypes = {
    requests: PropTypes.arrayOf(PropTypes.object).isRequired,
    fetchRequests: PropTypes.func.isRequired
  };

  constructor(props) {
    super(props);
    this.state = {
      requestTypeFilters: RequestFilters.REQUEST_TYPES,
      loading: false
    };
  }

  getCurrentEventAndTime(request) {
    let lastEvent;
    let lastEventTime = 0;
    if (request.requestDeployState && request.requestDeployState.pendingDeploy) {
      const pendingDeploy = request.requestDeployState.pendingDeploy;
      lastEvent = (
        <Link to={`request/${pendingDeploy.requestId}/deploy/${pendingDeploy.deployId}`}>
          <strong>Deploy</strong> - {pendingDeploy.deployId}{pendingDeploy.user && ` by ${pendingDeploy.user}`} is pending
        </Link>
      );
      lastEventTime = request.requestDeployState.pendingDeploy.timestamp
    }
    if (request.requestDeployState && request.requestDeployState.activeDeploy && request.requestDeployState.activeDeploy.timestamp > lastEventTime) {
      const activeDeploy = request.requestDeployState.activeDeploy;
      lastEvent = (
        <p>
          <strong>Deployed</strong> {Utils.timestampFromNow(activeDeploy.timestamp)}{activeDeploy.user && ` by ${activeDeploy.user}`}
        </p>
      );
      lastEventTime = request.requestDeployState.activeDeploy.timestamp
    }
    if (request.expiringBounce && request.expiringBounce.startMillis > lastEventTime) {
      lastEvent = (
        <p><strong>Bouncing</strong> -{request.expiringBounce.user && ` started by ${request.expiringBounce.user}`} {Utils.timestampFromNow(request.expiringBounce.startMillis)}</p>
      );
      lastEventTime = request.expiringBounce.startMillis;
    }
    if (request.expiringPause && request.expiringPause.startMillis > lastEventTime) {
      lastEvent = (
        <p><strong>Paused</strong>{request.expiringPause.user && ` by ${request.expiringPause.user}`} {Utils.timestampFromNow(request.expiringPause.startMillis)} will unpause in {Utils.duration(request.expiringPause.expiringAPIRequestObject.durationMillis)}</p>
      );
      lastEventTime = request.expiringPause.startMilli;
    }
    if (request.expiringScale && request.expiringScale.startMillis > lastEventTime) {
      lastEvent = (
        <p><strong>Scaled</strong>{request.expiringScale.user && ` by ${request.expiringScale.user}`} to "{request.request.instances}" {Utils.timestampFromNow(request.expiringScale.startMillis)}, will revert to "{request.expiringScale.revertToInstances}" in {Utils.duration(request.expiringScale.expiringAPIRequestObject.durationMillis)}</p>
      );
      lastEventTime = request.expiringScale.startMillis;
    }
    if (request.expiringSkipHealthchecks && request.expiringSkipHealthchecks.startMillis > lastEventTime) {
      lastEvent = (
        <p><strong>Healthchecks disabled</strong>{request.expiringSkipHealthchecks.user && ` by ${request.expiringSkipHealthchecks.user}`} {Utils.timestampFromNow(request.expiringSkipHealthchecks.startMillis)}, will enable in {Utils.duration(request.expiringSkipHealthchecks.expiringAPIRequestObject.durationMillis)}</p>
      );
      lastEventTime = request.expiringSkipHealthchecks.startMillis;
    }
    if (request.lastHistory && request.lastHistory.createdAt > lastEventTime) {
      lastEvent = (
        <p><strong>{Utils.humanizeText(request.lastHistory.eventType)}</strong> {Utils.timestampFromNow(request.lastHistory.createdAt)} {request.lastHistory.user && `by ${request.lastHistory.user}`} </p>
      );
      lastEventTime = request.lastHistory.createdAt;
    }
    if (request.request.requestType != 'SERVICE' && request.request.requestType != 'WORKER' && request.mostRecentTask && request.mostRecentTask.updatedAt > lastEventTime) {
      lastEvent = (
        <p>
          <Link to={`task/${request.mostRecentTask.taskId.id}`}>
            <strong>{Utils.humanizeText(request.mostRecentTask.lastTaskState)}</strong>
          </Link>
          {` ${Utils.timestampFromNow(request.mostRecentTask.updatedAt)}`}
        </p>
      );
      lastEventTime = request.mostRecentTask.updatedAt;
    }
    if (!lastEvent) {
      lastEvent = (<p>No Recent Activity</p>);
    }
    return {
      lastEvent: lastEvent,
      lastEventTime: lastEventTime
    }
  }

  toggleRequestType(requestType) {
    let selected = this.state.requestTypeFilters;
    if (selected.length === RequestFilters.REQUEST_TYPES.length) {
      selected = [requestType];
    } else if (_.isEmpty(_.without(selected, requestType))) {
      selected = RequestFilters.REQUEST_TYPES;
    } else if (_.contains(selected, requestType)) {
      selected = _.without(selected, requestType);
    } else {
      selected.push(requestType);
    }
    this.setState({
      requestTypeFilters: selected,
      loading: true
    })
    this.props.fetchRequests(selected).then(() => {
      this.setState({
        loading: false
      })
    })

  }

  render() {
    const summaryColumn = (
      <Column
        label="Recent Activity"
        id="summary"
        key="summary"
        sortable={true}
        cellData={(rowData) => this.getCurrentEventAndTime(rowData).lastEventTime}
        cellRender={
          (cellData, rowData) => {
            return (
              <RequestSummaryBox 
                request={rowData}
                currentEvent={this.getCurrentEventAndTime(rowData).lastEvent}
              />
            );
          }
        }
      />
    );

    const filterItems = RequestFilters.REQUEST_TYPES.map((requestType, index) => {
      const isActive = _.contains(this.state.requestTypeFilters, requestType);
      return (
        <li key={index} className={isActive ? 'active' : ''}>
          <a onClick={() => this.toggleRequestType(requestType)}>
            {isActive ? <RequestTypeIcon requestType={requestType} /> : <RequestTypeIcon requestType={requestType} translucent={true} />} {Utils.humanizeText(requestType)} 
          </a>
        </li>
      );
    });

    let table;

    if (this.state.loading) {
      table = <Loader />;
    } else {
      table = (
        <UITable
          data={this.props.requests}
          keyGetter={(requestParent) => requestParent.request.id}
          paginated={false}
          defaultSortBy="summary"
          defaultSortDirection={UITable.SortDirection.ASC}
          renderAllRows={true}
        >
          {[
            Cols.Starred,
            Cols.Type,
            Cols.State,
            Cols.RequestId,
            summaryColumn,
            Cols.Actions
          ]}
        </UITable>
      );
    }

    return (
      <div>
        <header className="detail-header">
          <Row>
            <Col md={3}>
              <h2>My Requests</h2>
            </Col>
            <Col md={9}>
              <div className="requests-filter-container pull-right">
                <ul className="nav nav-pills nav-pills-multi-select">
                  {filterItems}
                </ul>
              </div>
            </Col>
          </Row>
        </header>
        {table}
      </div>
    );
  }
}

function mapStateToProps(state) {
  const modifiedRequests = state.api.userRelevantRequests.data.map((request) => {
    const hasActiveDeploy = !!(request.activeDeploy || (request.requestDeployState && request.requestDeployState.activeDeploy));
    return {
      ...request,
      hasActiveDeploy,
      canBeRunNow: request.state === 'ACTIVE' && _.contains(['SCHEDULED', 'ON_DEMAND'], request.request.requestType) && hasActiveDeploy,
      canBeScaled: _.contains(['ACTIVE', 'SYSTEM_COOLDOWN'], request.state) && hasActiveDeploy && _.contains(['WORKER', 'SERVICE'], request.request.requestType),
      id: request.request ? request.request.id : request.requestId
    };
  });
  return {requests: modifiedRequests}
}

function mapDispatchToProps(dispatch) {
  return {
    fetchRequests: (requestTypes) => dispatch(FetchUserRelevantRequests.trigger(requestTypes))
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(DashboardPage, refresh));
