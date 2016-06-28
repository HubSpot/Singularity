import React from 'react';
import { connect } from 'react-redux';

import { FetchAction } from '../../actions/api/requests';

class RequestsPage extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      filter: {
        subFilter: props.subFilter,
        searchFilter: props.searchFilter,
        loading: false
      }
    }
  }

  render() {
    console.log(this.props);
    return <span>Hi</span>
  }
}

function mapStateToProps(state, ownProps) {
  return {
    requests: state.api.requests.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchFilter: (state) => dispatch(FetchAction.trigger(state))
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(RequestsPage);
