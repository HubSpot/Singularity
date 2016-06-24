import React, { Component, PropTypes } from 'react';
import Button from 'react-bootstrap/lib/Button';
import Glyphicon from 'react-bootstrap/lib/Glyphicon';

export default class RemoveButton extends Component {
  static propTypes = {
    onClick: PropTypes.func.isRequired,
    id: PropTypes.string.isRequired
  };

  render() {
    return (
      <Button
        className='remove-button'
        id={this.props.id}
        onClick={(event) => {
          event.preventDefault();
          this.props.onClick();
        }}/>
    );
  }
}
