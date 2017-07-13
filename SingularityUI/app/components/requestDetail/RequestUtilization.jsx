import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import CollapsableSection from '../common/CollapsableSection';
import { UsageInfo } from '../common/statelessComponents';
import Utils from '../../utils';
import BootstrapTable from 'react-bootstrap/lib/Table';

const RequestUtilization = ({isFetching, utilization}) => {
  const attributes = utilization && (
      <div className="row">
        <div className="col-md-3">
          <UsageInfo
            title="CPU per task average"
            total={utilization.cpuReserved / utilization.numTasks}
            used={utilization.cpuUsed / utilization.numTasks}
            style={utilization.cpuUsed >= utilization.cpuReserved && 'danger'}
          >
            <p>{Utils.roundTo(utilization.cpuUsed / utilization.numTasks, 2)} of {utilization.cpuReserved / utilization.numTasks} CPU reserved</p>
            <BootstrapTable responsive={false} striped={true} style={{marginTop: '10px'}}>
              <tbody>
                <tr>
                  <td>Min CPU (all tasks)</td>
                  <td>{Utils.roundTo(utilization.minCpuUsed, 2)}</td>
                </tr>
                <tr>
                  <td>Max CPU (all tasks)</td>
                  <td>{Utils.roundTo(utilization.maxCpuUsed, 2)}</td>
                </tr>
              </tbody>
            </BootstrapTable>
          </UsageInfo>
        </div>
        <div className="col-md-3">
          <UsageInfo
            title="Memory per task average"
            total={utilization.memBytesReserved / utilization.numTasks}
            used={utilization.memBytesUsed / utilization.numTasks}
            style={utilization.memBytesUsed >= utilization.memBytesReserved && 'danger'}
          >
            <p>{Utils.humanizeFileSize(utilization.memBytesUsed / utilization.numTasks)} of {Utils.humanizeFileSize(utilization.memBytesReserved / utilization.numTasks)} reserved</p>
            <BootstrapTable responsive={false} striped={true} style={{marginTop: '10px'}}>
              <tbody>
                <tr>
                  <td>Min memory (all tasks)</td>
                  <td>{Utils.humanizeFileSize(utilization.minMemBytesUsed)}</td>
                </tr>
                <tr>
                  <td>Max memory (all tasks)</td>
                  <td>{Utils.humanizeFileSize(utilization.maxMemBytesUsed)}</td>
                </tr>
              </tbody>
            </BootstrapTable>
          </UsageInfo>
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
