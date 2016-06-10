import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import * as RequestsActions from '../../actions/api/requests';
import * as RequestsPageActions from '../../actions/ui/requestsPage';

import Sidebar from '../common/Sidebar';
import MainContent from '../common/MainContent';
import TabBar from '../common/TabBar';
import TabBarFilterOption from '../common/tabBar/TabBarFilterOption';

import FilterOptionState from '../../containers/requests/FilterOptionState';
import FilterOptionType from '../../containers/requests/FilterOptionType';
import FilterSearchBar from '../../containers/requests/FilterSearchBar';
import FilteredRequestsTable from '../../containers/requests/FilteredRequestsTable';

class RequestsPage extends Component {
  constructor(props) {
    super(props);
    this.displayName = 'RequestsPage';
  }

  render() {
    const { requests } = this.props;

    return (
      <div className='requests-page'>
        <Sidebar>
          <FilterOptionState
            label={'Active'}
            filterValue={'ACTIVE'}
          />
          <FilterOptionState
            label={'Cooling down'}
            filterValue={'SYSTEM_COOLDOWN'}
          />
          <FilterOptionState
            label={'Paused'}
            filterValue={'PAUSED'}
          />
        </Sidebar>
        <MainContent>
          <TabBar>
            <FilterOptionType
              label={'All types'}
              filterValue={'ALL'}
            />
            <FilterOptionType
              label={'On-demand'}
              filterValue={'ON_DEMAND'}
            />
            <FilterOptionType
              label={'Worker'}
              filterValue={'WORKER'}
            />
            <FilterOptionType
              label={'Scheduled'}
              filterValue={'SCHEDULED'}
            />
            <FilterOptionType
              label={'Run-once'}
              filterValue={'RUN_ONCE'}
            />
            <FilterOptionType
              label={'Service'}
              filterValue={'SERVICE'}
            />
          </TabBar>
          <FilterSearchBar />
          <FilteredRequestsTable maxVisible={10} />
        </MainContent>
      </div>
    );
  }
}

export default RequestsPage;
