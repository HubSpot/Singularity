import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';
import { Link } from 'react-router';

import Utils from '../../utils';

import { refresh } from '../../actions/ui/dashboard';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import * as Cols from '../requests/Columns';
import RequestSummaryBox from './RequestSummaryBox';

class DashboardPage extends Component {
  static propTypes = {
    requests: PropTypes.arrayOf(PropTypes.object).isRequired
  };

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
    if (!lastEvent) {
      lastEvent = "No Recent Activity";
    }
    return {
      lastEvent: lastEvent,
      lastEventTime: lastEventTime
    }
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

    return (
      <UITable
        data={this.props.requests}
        keyGetter={(requestParent) => requestParent.request.id}
        paginated={false}
        defaultSortBy="summary"
        defaultSortDirection={UITable.SortDirection.ASC}
      >
        {[
          Cols.Starred,
          Cols.Type,
          Cols.RequestId,
          summaryColumn,
          Cols.Actions
        ]}
      </UITable>
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

export default connect(mapStateToProps)(rootComponent(DashboardPage, refresh));
