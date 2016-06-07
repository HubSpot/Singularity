import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import * as RequestsActions from '../../actions/requests';

import Sidebar from '../common/Sidebar';
import SidebarFilterOption from '../common/sidebar/SidebarFilterOption';
import MainContent from '../common/MainContent';
import TabBar from '../common/TabBar';
import TabBarFilterOption from '../common/tabBar/TabBarFilterOption';
import SearchBar from '../common/SearchBar';
import RequestsTable from '../requests/RequestsTable';

class RequestsPage extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestsPage';
  }

  render() {
    const { requests, actions } = this.props;
    return (
      <div className='requests-page'>
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
        <MainContent>
          <TabBar>
            <TabBarFilterOption
              isEnabled={true}
              filterName={'All types'}
              numberOfItems={0}
              onChange={console.log}
            />
            <TabBarFilterOption
              isEnabled={false}
              filterName={'On-demand'}
              numberOfItems={0}
              onChange={console.log}
            />
            <TabBarFilterOption
              isEnabled={false}
              filterName={'Worker'}
              numberOfItems={0}
              onChange={console.log}
            />
            <TabBarFilterOption
              isEnabled={false}
              filterName={'Scheduled'}
              numberOfItems={0}
              onChange={console.log}
            />
            <TabBarFilterOption
              isEnabled={false}
              filterName={'Run-once'}
              numberOfItems={0}
              onChange={console.log}
            />
            <TabBarFilterOption
              isEnabled={false}
              filterName={'Service'}
              numberOfItems={0}
              onChange={console.log}
            />
          </TabBar>
          <SearchBar />
          <RequestsTable />
        </MainContent>
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
