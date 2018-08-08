import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import CollapsableSection from '../common/CollapsableSection';
import { UsageInfo } from '../common/statelessComponents';
import Utils from '../../utils';
import BootstrapTable from 'react-bootstrap/lib/Table';
import Loader from "../common/Loader";

export const HUNDREDTHS_PLACE = 2;

const RequestUtilization = ({utilization}) => {
  const isCpuOverAllocated = utilization &&
    (utilization.maxCpuUsed > utilization.cpuReserved / utilization.numTasks);
  const isCpuThrottled = utilization && utilization.percentCpuTimeThrottled > 0;
  const attributes = utilization && (
      <div className="row">
        <div className="col-md-3">
          <UsageInfo
            title="CPU per task average"
            total={utilization.cpuReserved / utilization.numTasks}
            used={utilization.cpuUsed / utilization.numTasks}
            style={utilization.cpuUsed >= utilization.cpuReserved ? 'danger' : null}
          >
            <p>{Utils.roundTo(utilization.cpuUsed / utilization.numTasks, HUNDREDTHS_PLACE)} of {utilization.cpuReserved / utilization.numTasks} CPU reserved</p>
            <BootstrapTable responsive={false} striped={true} style={{marginTop: '10px'}}>
              <tbody>
                <tr>
                  <td>Min CPU (24 Hr Min)</td>
                  <td>{Utils.roundTo(utilization.minCpuUsed, HUNDREDTHS_PLACE)}</td>
                </tr>
                <tr>
                  <td className={isCpuOverAllocated ? 'danger' : ''}>Max CPU (24 Hr Max)</td>
                  <td className={isCpuOverAllocated ? 'danger' : ''}>{Utils.roundTo(utilization.maxCpuUsed, HUNDREDTHS_PLACE)}</td>
                </tr>
              </tbody>
            </BootstrapTable>
          </UsageInfo>
        </div>
        <div className="col-md-3">
          <UsageInfo
            title="Averge % CPU Time Throttled"
            total={100}
            used={utilization.percentCpuTimeThrottled / utilization.numTasks}
            style={isCpuThrottled ? 'danger' : null}
          >
            <BootstrapTable responsive={false} striped={true} style={{marginTop: '10px'}}>
              <tbody>
                <tr>
                  <td>Min CPU Time Throttled % (24 Hr Min)</td>
                  <td>{Utils.roundTo(utilization.minPercentCpuTimeThrottled, HUNDREDTHS_PLACE)}</td>
                </tr>
                <tr>
                  <td className={isCpuThrottled ? 'danger' : ''}>Max CPU Time Throttled % (24 Hr Max)</td>
                  <td className={isCpuThrottled ? 'danger' : ''}>{Utils.roundTo(utilization.maxPercentCpuTimeThrottled, HUNDREDTHS_PLACE)}</td>
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
            style={utilization.memBytesUsed >= utilization.memBytesReserved ? 'danger' : null}
          >
            <p>{Utils.humanizeFileSize(utilization.memBytesUsed / utilization.numTasks)} of {Utils.humanizeFileSize(utilization.memBytesReserved / utilization.numTasks)} reserved</p>
            <BootstrapTable responsive={false} striped={true} style={{marginTop: '10px'}}>
              <tbody>
                <tr>
                  <td>Min memory (24 Hr Min)</td>
                  <td>{Utils.humanizeFileSize(utilization.minMemBytesUsed)}</td>
                </tr>
                <tr>
                  <td>Max memory (24 Hr Max)</td>
                  <td>{Utils.humanizeFileSize(utilization.maxMemBytesUsed)}</td>
                </tr>
              </tbody>
            </BootstrapTable>
          </UsageInfo>
        </div>
        <div className="col-md-3">
          <UsageInfo
            title="Disk per task average"
            total={utilization.diskBytesReserved / utilization.numTasks}
            used={utilization.diskBytesUsed / utilization.numTasks}
            style={utilization.diskBytesUsed >= utilization.diskBytesReserved ? 'danger' : null}
          >
            <p>{Utils.humanizeFileSize(utilization.diskBytesUsed / utilization.numTasks)} of {Utils.humanizeFileSize(utilization.diskBytesReserved / utilization.numTasks)} reserved</p>
            <BootstrapTable responsive={false} striped={true} style={{marginTop: '10px'}}>
              <tbody>
              <tr>
                <td>Min disk (24 Hr Min)</td>
                <td>{Utils.humanizeFileSize(utilization.minDiskBytesUsed)}</td>
              </tr>
              <tr>
                <td>Max disk (24 Hr Max)</td>
                <td>{Utils.humanizeFileSize(utilization.maxDiskBytesUsed)}</td>
              </tr>
              </tbody>
            </BootstrapTable>
          </UsageInfo>
        </div>
    </div>
  );

  return (
    <CollapsableSection id="request-utilization" title="Resource usage" defaultExpanded={isCpuOverAllocated}>
      {attributes}
    </CollapsableSection>
  );
};

RequestUtilization.propTypes = {
  requestId: PropTypes.string.isRequired,
  utilization: PropTypes.object
};


const mapStateToProps = function(state, ownProps) {
  const requestId = ownProps.requestId;
  return {
    utilization: Utils.maybe(state, ['api', 'requestUtilization', requestId, 'data'])
  };
};

export default connect(
  mapStateToProps
)(RequestUtilization);
