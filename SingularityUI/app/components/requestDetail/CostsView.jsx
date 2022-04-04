import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Col } from 'react-bootstrap';

import CollapsableSection from '../common/CollapsableSection';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import Utils from '../../utils';

const CostsView = ({requestId, costsAPI}) => {
  const costs = costsAPI ? costsAPI.data : [];
  const title = costs.length ? 'Average Daily Costs ($' + costs.map((c) => c.cost).reduce((p, c) => p + c).toFixed(4) + ')' : 'Average Daily Costs';
  return  (
    <CollapsableSection id="costs" title={title} defaultExpanded={true}>
      <UITable
        data={costs}
        keyGetter={(c) => c.activityType + c.cost + c.costKey+ c.costType}
        defaultSortBy={'cost'}
        defaultSortDirection={'DESC'}
        showPageLoaderWhenFetching={true}
        isFetching={!costs.length}
      >
        <Column
          label="Activity Type"
          id="activityType"
          key="activityType"
          cellData={(c) => Utils.humanizeText(c.activityType)}
        />
        <Column
          label="Cost Primary Key"
          id="costKey"
          key="costKey"
          cellData={(c) => c.primaryKey}
        />
        <Column
          label="Cost Type"
          id="costType"
          key="costType"
          cellData={(c) => Utils.humanizeText(c.costType)}
        />
        <Column
          label="Cost"
          id="cost"
          key="cost"
          forceSortHeader={true}
          cellData={(c) => c.cost}
          cellRender={(c) => '$' + c}
        />
      </UITable>
    </CollapsableSection>
  );
}

CostsView.propTypes = {
  requestId: PropTypes.string.isRequired,
  costsAPI: PropTypes.object
};

const mapStateToProps = (state, ownProps) => ({
  costsAPI: Utils.maybe(state.api.costs, [ownProps.requestId])
});

export default connect(
  mapStateToProps
)(CostsView);