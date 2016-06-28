import React, { Component, PropTypes } from 'react';
import Humanize from 'humanize-plus';
import moment from 'moment';

import RequestPropTypes from '../../constants/api/RequestPropTypes';

import RequestTag from './RequestTag';
import RequestScheduleTag from './RequestScheduleTag';

class RequestsTableRow extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestsTableRow';
  }

  navigateToRequest(requestId) {
    app.router.navigate(`/request/${ requestId }`, { trigger: true });
  }

  getTagColumn() {
    const request = this.props.requestParent.request;
    return <RequestTag of={request.requestType}/>;
  }

  getPrimaryColumn() {
    const request = this.props.requestParent.request;
    return (
      <a
        className='request-name'
        onClick={() => this.navigateToRequest(request.id)}>
        {request.id}
      </a>
    );
  }

  getStateColumn() {
    const state = this.props.requestParent.state;
    return <RequestTag of={state}/>;
  }

  getInstancesColumn() {
    const request = this.props.requestParent.request;
    let maybeInstances = <span className='faded-value'>No instances</span>;
    if ("instances" in request) {
      maybeInstances = (
        <span>
          <span className='important-value'>
            {request.instances}
          </span>
          {' '}
          {Humanize.pluralize(request.instances, 'instance')}
        </span>
      );
    }

    return maybeInstances;
  }

  getDeployColumn() {
    const requestParent = this.props.requestParent;
    let maybeDeploy = <span>?</span>;
    if ('requestDeployState' in requestParent) {
      let whenDeployed = 'Not deployed';
      let maybeDeployUser = <span></span>;

      const requestDeployState = requestParent.requestDeployState;
      if ('activeDeploy' in requestDeployState) {
        // successful deployment that is now active
        const activeDeploy = requestDeployState.activeDeploy;

        whenDeployed = moment(activeDeploy.timestamp).fromNow();

        if ('user' in activeDeploy) {
          const deployUser = activeDeploy.user.split('@')[0];

          maybeDeployUser = (
            <span>
              {' by '}
              <span className='important-value'>{deployUser}</span>
            </span>
          );
        }
      }

      maybeDeploy = (
        <span>
          <span>
            {whenDeployed}
          </span>
          {maybeDeployUser}
          <RequestScheduleTag
            request={requestParent.request}
          />
        </span>
      );
    }

    return maybeDeploy;
  }

  render() {
    return (
      <tr className='table-row'>
        <td>{this.getTagColumn()}</td>
        <td>{this.getPrimaryColumn()}</td>
        <td>{this.getStateColumn()}</td>
        <td>{this.getInstancesColumn()}</td>
        <td>{this.getDeployColumn()}</td>
        <td>
          <span className='action-links'>
            <a href='#'>Delete</a>|
            <a href='#'>Scale</a>|
            <a href='#'>Edit</a>
          </span>
        </td>
      </tr>
    );
  }
}

RequestsTableRow.propTypes = {
  requestParent: RequestPropTypes.RequestParent.isRequired
}

export default RequestsTableRow;
