import { buildJsonApiAction } from './base';

export const EnableRackSensitivity = buildJsonApiAction(
  'ENABLE_RACK_SENSITIVITY',
  'POST',
  {
    url: '/disasters/rack-sensitive/enable',
    body: {}
  }
);

export const DisableRackSensitivity = buildJsonApiAction(
  'DISABLE_RACK_SENSITIVITY',
  'POST',
  {
    url: '/disasters/rack-sensitive/disable',
    body: {}
  }
);


export const OverridePlacementStrategy = buildJsonApiAction(
  'SET_PLACEMENT_STRATEGY',
  'POST',
  strategy => {
    if (!strategy) {
      strategy = '';
    }

    return {
      url: `/disasters/placement-strategy/override/set/${strategy}`,
      body: {}
    };
  }
);
