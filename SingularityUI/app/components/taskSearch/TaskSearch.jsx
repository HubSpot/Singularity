import React from 'react';
import { connect } from 'react-redux';

class TaskSearch extends React.Component {

  render() {
    return (
      <div>
        {this.props.requestId}
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {};
}

function mapDispatchToProps(dispatch) {
  return {};
}

export default connect(mapStateToProps, mapDispatchToProps)(TaskSearch);
