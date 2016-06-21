import React, { Component, PropTypes } from 'react';

class RequestCount extends Component {
  static propTypes = {
    label: PropTypes.string.isRequired,
    count: PropTypes.number.isRequired,
    link: PropTypes.string
  };

  constructor(props) {
    super(props);
    this.displayName = 'RequestCount';
  }

  render() {
    return (
      <a className='big-number-link' href={this.props.link}>
        <div className='well'>
          <div className='big-number'>
            <div className='number' data-state-attribute='requests'>
              {this.props.count}
            </div>
            <div className='number-label'>{this.props.label}</div>
          </div>
        </div>
      </a>
    );
  }
}

export default RequestCount;
