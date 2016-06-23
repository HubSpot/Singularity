import React, { Component, PropTypes } from 'react';

import ConfirmModal from '../common/ConfirmModal';

export default class UnpauseButton extends Component {
  static propTypes = {
    requestId: PropTypes.string.isRequired,
    unpauseAction: PropTypes.func.isRequired
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
    this.props.unpauseAction(this.props.requestId, this.state.message);
  }

  render() {
    return (
      <ConfirmModal
        confirm={this.confirm.bind(this)}
        alt='Unpause Request'
        data-action='unpause'
        button={<span className='glyphicon glyphicon-play'></span>}
      >
        <p>Are you sure you want to unpause the request?</p>
        <pre>{this.props.requestId}</pre>
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
      </ConfirmModal>
    );
  }
}
