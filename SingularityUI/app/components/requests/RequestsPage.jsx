import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import * as RequestsActions from '../../actions/requests';

import Sidebar from '../common/Sidebar';
import SidebarFilterOption from '../common/sidebar/SidebarFilterOption';
import SearchBar from '../common/SearchBar';
import TabBar from '../common/TabBar';

class RequestsPage extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestsPage';
  }

  render() {
    const { requests, actions } = this.props;
    return (
      <div>
        <Sidebar>
          <SidebarFilterOption
            isEnabled={true}
            filterName={'Active'}
            indicatorClass={'state-indicator active'}
            numberOfItems={0}
            onChange={console.log}
          />
          <SidebarFilterOption
            isEnabled={true}
            filterName={'Cooling down'}
            indicatorClass={'state-indicator cooling-down'}
            numberOfItems={0}
            onChange={console.log}
          />
          <SidebarFilterOption
            isEnabled={true}
            indicatorClass={'state-indicator paused'}
            filterName={'Paused'}
            numberOfItems={0}
            onChange={console.log}
          />
        </Sidebar>
        <div>
          <TabBar />
          <SearchBar />
          <div>Table goes here</div>
        </div>
      </div>
    );
  }
}

RequestsPage.propTypes = {
  requests: PropTypes.object.isRequired,
  actions: PropTypes.object.isRequired
};

const mapStateToProps = (state) => {
  return {
    requests: state.requests
  }
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(RequestsActions, dispatch)
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestsPage);
