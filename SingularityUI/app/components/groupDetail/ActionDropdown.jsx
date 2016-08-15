import React, {PropTypes} from 'react';
import { connect } from 'react-redux';

import { DropdownButton, MenuItem } from 'react-bootstrap';
import PauseButton from '../requests/PauseButton';
import UnpauseButton from '../requests/UnpauseButton';
import BounceButton from '../requests/BounceButton';

class ActionDropdown extends React.Component {

  static propTypes = {
    group: PropTypes.object.isRequired,
    requests: PropTypes.object
  }

  constructor() {
    super();
    this.state = {
      dropdownOpen: false
    };
    _.bindAll(this, 'onMenuClick');
  }

  onMenuClick() {
    this.setState({
      dropdownOpen: !this.state.dropdownOpen
    });
  }

  render() {
    const { group, requests } = this.props;

    return (
      <DropdownButton
        bsStyle="primary"
        title="Apply to all"
        id="action-dropdown"
        open={this.state.dropdownOpen}
        onToggle={_.noop}
        onClick={this.onMenuClick}
        >
        <PauseButton requestId={group.requestIds} isScheduled={_.any(_.keys(requests), (requestId) => requests[requestId].requestType === 'SCHEDULED')}>
          <MenuItem eventKey="1">Pause</MenuItem>
        </PauseButton>
        <UnpauseButton requestId={group.requestIds}>
          <MenuItem eventKey="2">Unpause</MenuItem>
        </UnpauseButton>
        <BounceButton requestId={group.requestIds}>
          <MenuItem eventKey="3">Bounce</MenuItem>
        </BounceButton>
      </DropdownButton>
    );
  }
}

const mapStateToProps = (state, ownProps) => {
  const requests = ownProps.group && _.pick({...state.api.request}, (value, key) => _.contains(ownProps.group.requestIds, key));
  return ({
    requests
  });
};

export default connect(mapStateToProps, null)(ActionDropdown);
