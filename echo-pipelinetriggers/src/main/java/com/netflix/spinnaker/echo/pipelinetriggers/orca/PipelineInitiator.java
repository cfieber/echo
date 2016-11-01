package com.netflix.spinnaker.echo.pipelinetriggers.orca;

import javax.annotation.PostConstruct;

import com.netflix.spinnaker.echo.config.OrcaConfigurationProperties;
import com.netflix.spinnaker.echo.model.Pipeline;
import com.netflix.spinnaker.echo.pipelinetriggers.orca.OrcaService.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.functions.Action1;

/**
 * Triggers a {@link Pipeline} by invoking _Orca_.
 */
@Component
@Slf4j
public class PipelineInitiator implements Action1<Pipeline> {

  private final CounterService counter;
  private final OrcaService orca;
  private final boolean enabled;

  @Autowired
  public PipelineInitiator(CounterService counter,
                           OrcaService orca,
                           OrcaConfigurationProperties orcaConfigurationProperties) {
    this.counter = counter;
    this.orca = orca;
    this.enabled = orcaConfigurationProperties.isEnabled();
  }

  @PostConstruct
  public void initialize() {
    if (!enabled) {
      log.warn("Orca triggering is disabled");
    }
  }

  @Override
  public void call(Pipeline pipeline) {
    if (enabled) {
      log.warn("Triggering {} due to {}", pipeline, pipeline.getTrigger());
      counter.increment("orca.requests");

      String runAsUser = null;
      if (pipeline.getTrigger() != null) {
        runAsUser = pipeline.getTrigger().getRunAsUser();
      }

      Observable<OrcaService.Response> response;
      if (runAsUser != null && !runAsUser.isEmpty()) {
        response = orca.trigger(pipeline, pipeline.getTrigger().getRunAsUser());
      } else {
        response = orca.trigger(pipeline);
      }
      response.subscribe(this::onOrcaResponse, this::onOrcaError);
    } else {
      log.info("Would trigger {} due to {} but triggering is disabled", pipeline, pipeline.getTrigger());
    }
  }

  private void onOrcaResponse(Response response) {
    log.info("Triggered pipeline {}", response.getRef());
  }

  private void onOrcaError(Throwable error) {
    log.error("Error triggering pipeline", error);
    counter.increment("orca.errors");
  }
}
