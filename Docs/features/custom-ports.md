### Choosing Custom Ports

As of release `0.4.10`, you can specify an index for the dynamically allocated port Singularity should use when healthchecking and when adding the service to the load balancer. Previously, Singularity would always use the first dynamically allocated port.

To change the healthcheck port, simply add:

```json
"healthcheck": {
  "portIndex": 1 # or another integer
}
```

to your `SingularityDeploy` object. This will tell Singularity to use the dynamically allocated port at index 1 (i.e. second allocated port) when performing a health check. Alternatively, you can specify a specif port the use for the healthcheck with an option like:
```json
"healthcheck": {
  "portNumberx": 80 # or another integer
}
```

Similarly, you can also specify the port index to use for the load balancer by specifying:

```json
"loadBalancerPortIndex": 1 # or another integer
```

in your `SingularityDeploy` object. Keep in mind the dynamically all ocated ports will be available to your process as environment variables in the format `PORT{index}` (e.g. `PORT0=32091` for a first dynamically allocated port of 32091)
