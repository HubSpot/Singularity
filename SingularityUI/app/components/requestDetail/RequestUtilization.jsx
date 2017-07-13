import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import CollapsableSection from '../common/CollapsableSection';
import { InfoBox, UsageInfo } from '../common/statelessComponents';
import Utils from '../../utils';

const RequestUtilization = ({isFetching, utilization}) => {
  const attributes = utilization && (
      <div className="row">
        <div className="col-md-3">
          <UsageInfo
            title="CPU per task average"
            total={utilization.cpuReserved / utilization.numTasks}
            used={utilization.cpuUsed / utilization.numTasks}
          >
            <span>{Utils.roundTo(utilization.cpuUsed / utilization.numTasks, 2)} of {utilization.cpuReserved / utilization.numTasks} CPU reserved</span>
          </UsageInfo>
          <UsageInfo
            title="Memory per task average"
            total={utilization.memBytesReserved / utilization.numTasks}
            used={utilization.memBytesUsed / utilization.numTasks}
          >
            <span>{Utils.humanizeFileSize(utilization.memBytesUsed / utilization.numTasks)} of {Utils.humanizeFileSize(utilization.memBytesReserved / utilization.numTasks)} reserved</span>
          </UsageInfo>
        </div>
        <div className="col-md-9">
          <ul className="list-unstyled horizontal-description-list">
            <InfoBox name="Min memory (all tasks)" value={Utils.humanizeFileSize(utilization.minMemBytesUsed)} />
            <InfoBox name="Max memory (all tasks)" value={Utils.humanizeFileSize(utilization.maxMemBytesUsed)} />
            <InfoBox name="Min CPU (all tasks)" value={Utils.roundTo(utilization.minCpuUsed, 2)} />
            <InfoBox name="Max CPU (all tasks)" value={Utils.roundTo(utilization.maxCpuUsed, 2)} />
          </ul>
        </div>
    </div>
  );

  return (
    <CollapsableSection id="request-utilization" title="Resource usage" subtitle="(past 24 hours)">
      {isFetching ? <div className="page-loader fixed" /> : attributes}
    </CollapsableSection>
  );
};

RequestUtilization.propTypes = {
  requestId: PropTypes.string.isRequired,
  isFetching: PropTypes.bool.isRequired,
  utilization: PropTypes.object
};


const mapStateToProps = function(state, ownProps) {
  const requestId = ownProps.requestId;
  return {
    isFetching: state.api.utilization.isFetching,
    utilization: _.find(state.api.utilization.data.requestUtilizations, (request) => {
      return request.requestId === requestId;
    })
  };
};

export default connect(
  mapStateToProps
)(RequestUtilization);
