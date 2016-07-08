import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Row, Col, Well, Alert, Button } from 'react-bootstrap';

import Utils from '../../../utils';

import { getBouncesForRequest } from '../../../selectors/tasks';

import CancelDeployButton from './CancelDeployButton';

const RequestAlerts = ({requestId, requestParent, bounces, activeTasksForRequest}) => {
  let maybeBouncing;
  if (bounces.length > 0) {
    const runningInstanceCount = Utils.request.runningInstanceCount(activeTasksForRequest);
    maybeBouncing = (
      <Alert bsStyle="warning">
        <b>Request is bouncing:</b> {runningInstanceCount} of {requestParent.request.instances} replacement tasks are currently running.
      </Alert>
    );
  }

  let maybeDeploying;
  const { pendingDeploy, activeDeploy } = requestParent;
  if (pendingDeploy) {
    const deployingInstanceCount = Utils.request.deployingInstanceCount(requestParent, activeTasksForRequest);
    const { instances } = requestParent.request;
    const pendingDeployProgress = (
      <span>{`${deployingInstanceCount} of ${instances} new tasks are currently running`}</span>
    );

    let maybeDeployProgress;
    let maybeAdvanceDeploy;

    const { pendingDeployState } = requestParent;
    if (pendingDeployState && pendingDeployState.deployProgress) {
      const { deployProgress } = pendingDeployState;
      const {
        targetActiveInstances,
        stepComplete,
        autoAdvanceDeploySteps
      } = deployProgress;

      if (targetActiveInstances === instances) {
        // all instances have launched, but it's still pending... wait for them to become healthy
        maybeDeployProgress = (
          <span>
            {pendingDeployProgress}
            <p>{deployingInstanceCount === instances ? ' Waiting for new tasks to become healthy.' : ''}</p>
          </span>
        );
      } else {
        maybeAdvanceDeploy = (
          <Button style={{float: 'right'}} bsStyle="primary" data-action="stepDeploy">
            Advance Deploy
          </Button>
        );
        // not all instances have launched, wait for that to happen
        if (stepComplete) {
          let nextDeployStepRemark;
          if (autoAdvanceDeploySteps) {
            const nextDeployStepTimestamp = deployProgress.timestamp + deployProgress.deployStepWaitTimeMs;
            nextDeployStepRemark = <span>next deploy step {Utils.timestampFromNow(nextDeployStepTimestamp)}</span>;
          } else {
            nextDeployStepRemark = <span>waiting for manual trigger of next deploy step.</span>;
          }
          maybeDeployProgress = (
            <span>
              Finished deploying {targetActiveInstances} of {instances} total instances, {nextDeployStepRemark}
            </span>
          );
        } else {
          maybeDeployProgress = (
            <span>
              {
                `Trying to deploy ${targetActiveInstances} of ${instances}
                instances, ${deployingInstanceCount} of
                ${targetActiveInstances} new tasks are currently running.`
              }
            </span>
          );
        }
      }
    }

    maybeDeploying = (
      <Well>
        <Row>
          <Col md={10} sm={8}>
            <b>Deploy </b>
            <code>
              <a href={`${config.appRoot}/request/${requestId}/deploy/${pendingDeploy.id}`}>
                {pendingDeploy.id}
              </a>
            </code>
            <b> is pending: </b>
            {maybeDeployProgress}
          </Col>
          <Col md={2} sm={4}>
            {maybeAdvanceDeploy}
            <CancelDeployButton deployId={pendingDeploy.id} requestId={requestId} />
          </Col>
        </Row>
      </Well>
    );
  }

  let maybeActiveDeploy;
  if (activeDeploy) {
    const deployedBy = Utils.maybe(activeDeploy, ['metadata', 'deployedBy']);

    let maybeDeployedBy;
    if (typeof deployedBy === 'string') {
      maybeDeployedBy = (
        <span> {deployedBy.split('@')[0]}</span>
      );
    }

    let maybeTimestamp;
    if (activeDeploy.timestamp) {
      maybeTimestamp = (
        <span> {Utils.timestampFromNow(activeDeploy.timestamp)}</span>
      );
    }

    maybeActiveDeploy = (
      <div>
        <span>Active deploy </span>
        <code>
          <a href={`${config.appRoot}/request/${requestId}/deploy/${activeDeploy.id}`}>
            {activeDeploy.id}
          </a>
        </code>
        {maybeDeployedBy}
        {maybeTimestamp}
      </div>
    );
  } else {
    maybeActiveDeploy = (
      <span className="text-danger">
        No active deploy
      </span>
    );
  }

  return (
    <div>
      {maybeBouncing}
      {maybeDeploying}
      <Well>
        <Row>
          <Col md={10} sm={8}>
            {maybeActiveDeploy}
          </Col>
          <Col md={2} sm={4}>
            <a href="#deploy-history" className="pull-right">
              Deploy history
            </a>
          </Col>
        </Row>
      </Well>
    </div>
  );
};

RequestAlerts.propTypes = {
  requestId: PropTypes.string.isRequired,
  requestParent: PropTypes.object.isRequired,
  bounces: PropTypes.arrayOf(PropTypes.object).isRequired,
  activeTasksForRequest: PropTypes.arrayOf(PropTypes.object).isRequired
};

const mapStateToProps = (state, ownProps) => {
  return {
    requestParent: Utils.maybe(state.api.request, [ownProps.requestId, 'data']),
    bounces: getBouncesForRequest(ownProps.requestId)(state),
    activeTasksForRequest: Utils.maybe(state.api, ['activeTasksForRequest', ownProps.requestId, 'data'])
  };
};


export default connect(
  mapStateToProps
)(RequestAlerts);
