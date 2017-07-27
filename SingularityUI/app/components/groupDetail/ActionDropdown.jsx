import React, {PropTypes} from 'react';
import { connect } from 'react-redux';

import { FetchRequest } from '../../actions/api/requests';
import {
  FetchActiveTasksForRequest,
  FetchRequestHistory
} from '../../actions/api/history'

import { DropdownButton, MenuItem, Glyphicon, Tooltip, OverlayTrigger } from 'react-bootstrap';
import PauseButton from '../common/modalButtons/PauseButton';
import UnpauseButton from '../common/modalButtons/UnpauseButton';
import BounceButton from '../common/modalButtons/BounceButton';
import EnableHealthchecksButton from '../common/modalButtons/EnableHealthchecksButton';
import DisableHealthchecksButton from '../common/modalButtons/DisableHealthchecksButton';

class ActionDropdown extends React.Component {

  static propTypes = {
    group: PropTypes.object.isRequired,
    requests: PropTypes.object,
    fetchRequest: PropTypes.func.isRequired,
    fetchRequestHistory: PropTypes.func.isRequired,
    fetchActiveTasksForRequest: PropTypes.func.isRequired,
    metadata: PropTypes.node
  };

  constructor() {
    super();
    this.state = {
      dropdownOpen: false
    };
    _.bindAll(this, 'onMenuClick', 'fetchRequestsAndHistory', 'fetchRequestsAndHistoryAndActiveTasks');
  }

  onMenuClick() {
    this.setState({
      dropdownOpen: !this.state.dropdownOpen
    });
  }

  fetchRequestsAndHistory() {
    for (const requestId of _.keys(this.props.requests)) {
      this.props.fetchRequest(requestId);
      this.props.fetchRequestHistory(requestId, 5, 1);
    }
  }

  fetchRequestsAndHistoryAndActiveTasks() {
    for (const requestId of _.keys(this.props.requests)) {
      this.props.fetchRequest(requestId);
      this.props.fetchRequestHistory(requestId, 5, 1);
      this.props.fetchActiveTasksForRequest(requestId);
    }
  }

  render() {
    const { group, requests, metadata } = this.props;

    const tooltip = (
      <Tooltip id="request-group-tooltip">Apply to all requests in group</Tooltip>
    );

    return (
      <OverlayTrigger placement="top" overlay={tooltip}>
        <DropdownButton
          bsStyle="default"
          bsSize="small"
          title={<Glyphicon glyph="option-vertical" />}
          id="action-dropdown"
          open={this.state.dropdownOpen}
          onToggle={_.noop}
          onClick={this.onMenuClick}
          noCaret={true}
        >
          <MenuItem header={true}>Request State</MenuItem>
          <PauseButton requestId={group.requestIds} isScheduled={_.any(_.keys(requests), (requestId) => requests[requestId].requestType === 'SCHEDULED')} then={this.fetchRequestsAndHistoryAndActiveTasks}>
            <MenuItem eventKey="1">Pause</MenuItem>
          </PauseButton>
          <UnpauseButton requestId={group.requestIds} then={this.fetchRequestsAndHistoryAndActiveTasks}>
            <MenuItem eventKey="2">Unpause</MenuItem>
          </UnpauseButton>
          <MenuItem divider={true} />

          <MenuItem header={true}>Healthchecks</MenuItem>
          <EnableHealthchecksButton requestId={group.requestIds} then={this.fetchRequestsAndHistory}>
            <MenuItem eventKey="3">Enable</MenuItem>
          </EnableHealthchecksButton>
          <DisableHealthchecksButton requestId={group.requestIds} then={this.fetchRequestsAndHistory}>
            <MenuItem eventKey="4">Disable</MenuItem>
          </DisableHealthchecksButton>
          <MenuItem divider={true} />

          <BounceButton requestId={group.requestIds} then={this.fetchRequestsAndHistoryAndActiveTasks}>
            <MenuItem eventKey="5">Bounce</MenuItem>
          </BounceButton>

          {metadata && <MenuItem divider={true} />}
          {metadata}
        </DropdownButton>
      </OverlayTrigger>
    );
  }
}

const mapStateToProps = (state, ownProps) => {
  const requests = ownProps.group && _.pick({...state.api.request}, (value, key) => _.contains(ownProps.group.requestIds, key));
  return ({
    requests
  });
};

const mapDispatchToProps = (dispatch) => ({
  fetchRequest: (requestId) => dispatch(FetchRequest.trigger(requestId)),
  fetchRequestHistory: (requestId, count, page) => dispatch(FetchRequestHistory.trigger(requestId, count, page)),
  fetchActiveTasksForRequest: (requestId) => dispatch(FetchActiveTasksForRequest.trigger(requestId))
});

export default connect(mapStateToProps, mapDispatchToProps)(ActionDropdown);
