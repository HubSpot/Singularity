import React, { Component, PropTypes } from 'react';
import { FormattedNumber, FormattedPlural } from 'react-intl';

import RequestTag from './RequestTag';

class RequestsTableRow extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestsTableRow';
  }

  render() {
    const requestParent = this.props.requestParent;
    const request = this.props.requestParent.request;
    const state = this.props.requestParent.state;

    let instancesMaybe = <span className='faded-value'>No instances</span>;
    if ("instances" in request) {
      // todo: pluralize
      instancesMaybe = (
        <span>
          <span className='important-value'>
            <FormattedNumber value={request.instances} />
          </span>
          {' '}
          <FormattedPlural value={request.instances}
            one='instance'
            other='instances'
          />
        </span>
      );
    }

    let deployMaybe = <span>?</span>;
    if ("activeDeploy" in requestParent) {
      let minutes = 0;
      let deployUser = 'me';

      deployMaybe = (
        <span>
          <span className='important-value'>
            {minutes}
          </span>
          {' minutes ago by '}
          <span className='important-value'>{deployUser}</span>
        </span>
      );
    }

    return (
      <tr className='table-row'>
        <td><RequestTag of={request.requestType}/></td>
        <td>
          <a className='request-name' href='#'>{request.id}</a>
          <br />
          <span className='subline'>Deployed by <a href='#'>XYZ</a></span>
        </td>
        <td><RequestTag of={state}/></td>
        <td>{instancesMaybe}</td>
        <td>{deployMaybe}</td>
        <td>
          <span className='action-links'>
            <a href='#'>Delete</a>|
            <a href='#'>Scale</a>|
            <a href='#'>View JSON</a>|
            <a href='#'>Edit</a>
          </span>
        </td>
      </tr>
    );
  }
}

RequestsTableRow.propTypes = {
  requestParent: PropTypes.shape({
    request: PropTypes.shape({
      id: PropTypes.string.isRequired,
      requestType: PropTypes.string.isRequired,
      owners: PropTypes.arrayOf(PropTypes.string),
      schedule: PropTypes.string,
      quartzSchedule: PropTypes.string,
      scheduleType: PropTypes.string,
      instances: PropTypes.number
    }).isRequired,
    state: PropTypes.string.isRequired,
    requestDeployState: PropTypes.shape({
      activeDeploy: PropTypes.shape({
        deployId: PropTypes.string.isRequired,
        timestamp: PropTypes.number.isRequired,
        user: PropTypes.string,
        message: PropTypes.string
      }),
      pendingDeploy: PropTypes.shape({
        deployId: PropTypes.string.isRequired,
        timestamp: PropTypes.number.isRequired,
        user: PropTypes.string,
        message: PropTypes.string
      })
    }),
  }).isRequired
}

export default RequestsTableRow;
