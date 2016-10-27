import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Section from '../common/Section';

import Utils from '../../utils';

import { Link } from 'react-router';

import { FetchDeploysForRequestWithMetaData } from '../../actions/api/history';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';

const DeployHistoryTable = ({requestId, deploysAPI, fetchDeploys}) => {
  const deploys = deploysAPI ? deploysAPI.data.objects : [];
  const isFetching = deploysAPI ? deploysAPI.isFetching : false;
  const emptyTableMessage = (Utils.api.isFirstLoad(deploysAPI)
    ? 'Loading...'
    : 'No deploys'
  );
  return (
    <Section id="deploy-history" title="Deploy history">
      <UITable
        emptyTableMessage={emptyTableMessage}
        data={deploys || []}
        totalResults={deploysAPI.data.dataCount}
        keyGetter={({deployMarker}) => deployMarker.deployId}
        paginated={true}
        page={deploysAPI.data.page}
        resultsPerPage={5}
        maxPage={deploysAPI.data.pageCount}
        fetchDataFromApi={(page, numberPerPage) => fetchDeploys(requestId, numberPerPage, page)}
        isFetching={isFetching}
      >
        <Column
          label="Deploy ID"
          id="deploy-id"
          key="deploy-id"
          cellData={(deploy) => (
            <Link to={`request/${deploy.deployMarker.requestId}/deploy/${deploy.deployMarker.deployId}`}>
              {deploy.deployMarker.deployId}
            </Link>
          )}
        />
        <Column
          label="Status"
          id="status"
          key="status"
          cellData={({deployResult}) => (deployResult ? Utils.humanizeText(deployResult.deployState) : 'Pending')}
        />
        <Column
          label="User"
          id="user"
          key="user"
          cellData={({deployMarker}) => deployMarker.user && deployMarker.user.split('@')[0] || 'N/A'}
        />
        <Column
          label="Timestamp"
          id="timestamp"
          key="timestamp"
          cellData={(deploy) => Utils.timestampFromNow(deploy.deployMarker.timestamp)}
        />
        <Column
          id="actions-column"
          key="actions-column"
          className="actions-column"
          cellData={(deploy) => <JSONButton object={deploy} showOverlay={true}>{'{ }'}</JSONButton>}
        />
      </UITable>
    </Section>
  );
};

DeployHistoryTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  deploysAPI: PropTypes.object.isRequired,
  fetchDeploys: PropTypes.func.isRequired
};

const mapStateToProps = (state, ownProps) => ({
  deploysAPI: Utils.maybe(
    state.api.deploysForRequest,
    [ownProps.requestId]
  )
});

const mapDispatchToProps = (dispatch) => ({
  fetchDeploys: (requestId, count, page) => dispatch(FetchDeploysForRequestWithMetaData.trigger(requestId, count, page))
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DeployHistoryTable);
