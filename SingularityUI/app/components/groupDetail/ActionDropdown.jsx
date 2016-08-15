import React, {PropTypes} from 'react';

import { DropdownButton, MenuItem } from 'react-bootstrap';
import PauseButton from '../requests/PauseButton';
import UnpauseButton from '../requests/UnpauseButton';
import BounceButton from '../requests/BounceButton';

export default class ActionDropdown extends React.Component {

  static propTypes = {
    group: PropTypes.object.isRequired,
    requests: PropTypes.object
  }

  constructor() {
    super();
    this.state = {
      dropdownOpen: false
    };
    _.bindAll(this, 'onMenuSelect');
  }

  onMenuSelect() {
    console.log('select');
  }

  render() {
    const { group, requests } = this.props;

    return (
      <DropdownButton
        bsStyle="primary"
        title="Apply to all"
        id="action-dropdown"
        open={this.state.dropdownOpen}
        onToggle={() => this.setState({dropdownOpen: true})}
        >
        <PauseButton requestId={group.requestIds} isScheduled={_.any(_.keys(requests), (requestId) => requests[requestId].requestType === 'SCHEDULED')}>
          <MenuItem eventKey="1" onSelect={this.onMenuSelect}>Pause</MenuItem>
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
