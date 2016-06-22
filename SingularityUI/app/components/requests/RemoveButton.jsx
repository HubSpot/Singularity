import React, { Component, PropTypes } from 'react';

import ModalButton from '../common/ModalButton';

export default class RemoveButton extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    removeAction: PropTypes.func.isRequired
  };

  constructor() {
    super();

    this.state = {
      message: ''
    };
  }

  handleChange(event) {
    this.setState({message: event.target.value});
  }

  confirm() {
    this.props.removeAction(this.props.requestId, this.state.message);
  }

  render() {
    return (
      <ModalButton
        confirm={this.confirm.bind(this)}
        alt='Remove Request'
        data-action='remove'
        button={<span className='glyphicon glyphicon-trash'></span>}
      >
        <p>Are you sure you want to remove this request?</p>
        <pre>{this.props.requestId}</pre>
        <p>If not paused, removing this request will kill all active and scheduled tasks and tasks for it will not run again unless it is reposted to Singularity.</p>
        <form>
          <div className='form-group'>
            <input
              className='form-control'
              type='text'
              value={this.state.message}
              onChange={this.handleChange.bind(this)}
              placeholder='Message (optional)'
            />
          </div>
        </form>
      </ModalButton>
    );
  }
}
