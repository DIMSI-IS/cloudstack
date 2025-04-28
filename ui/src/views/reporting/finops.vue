// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

<template>
  <a-row :gutter="12">
      <a-col :md="24">
      <a-card class="breadcrumb-card">
        <a-col :md="24" style="display: flex">
          <breadcrumb style="padding-top: 6px; padding-left: 8px" />
          <a-button
            style="margin-left: 12px; margin-top: 4px"
            :loading="loading"
            size="small"
            shape="round"
            @click="fetchData()" >
            <template #icon><ReloadOutlined /></template>
            {{ $t('label.refresh') }}
          </a-button>
        </a-col>
      </a-card>
    </a-col>
  </a-row>
  <div>
    <chart-card class="filter-card">
      <template #title>
        <div class="filter-container">
          <div class="select-container">
            <a-select
              id="type"
              style="width: 120px">
              <a-select-option value="User">User</a-select-option>
              <a-select-option value="Account">Account</a-select-option>
              <a-select-option value="Domain">Domain</a-select-option>
              <a-select-option value="Project">Project</a-select-option>
            </a-select>
          </div>
          <div class="select-container">
            <a-select
              id="subtype"
              style="width: 120px">
              <a-select-option value="user">User</a-select-option>
              <a-select-option value="account">Account</a-select-option>
              <a-select-option value="domain">Domain</a-select-option>
              <a-select-option value="project">Project</a-select-option>
            </a-select>
          </div>
          <div class="radio-container">
            <a-radio-group button-style="solid">
              <a-radio-button value="daily">Daily</a-radio-button>
              <a-radio-button value="weekly">Weekly</a-radio-button>
              <a-radio-button value="monthly">Monthly</a-radio-button>
              <a-radio-button value="yearly">Yearly</a-radio-button>
            </a-radio-group>
          </div>
        </div>
      </template>
    </chart-card>
    <div class="chart-container">
      <chart-card class="chart-card-left">
        <div class="center">
          <h3>
            <DashboardOutlined />
            CPU
            <InfoCircleOutlined class="info-icon" :title="$t('label.see.more.info.cpu.usage')"/>
          </h3>
        </div>
        <div v-if="loaded">
          <div v-if="chartLabels.length > 0">
            <resource-stats-line-chart
              :chartLabels="chartLabels"
              :chartData="resourceUsageHistory.cpu"
              :yAxisInitialMax="2250"
              :yAxisIncrementValue="10"
              :yAxisMeasurementUnit="'GHz'"
            />
          </div>
        </div>
      </chart-card>
      <chart-card class="chart-card-right">
        <div class="center">
          <h3>
            <DashboardOutlined />
            RAM
          </h3>
        </div>
        <div v-if="loaded">
            <div v-if="chartLabels.length > 0">
              <resource-stats-line-chart
                :chartLabels="chartLabels"
                :chartData="resourceUsageHistory.ram"
                :yAxisInitialMax="6"
                :yAxisIncrementValue="1"
                :yAxisMeasurementUnit="'TB'"
              />
            </div>
          </div>
      </chart-card>
    </div>
    <div class="chart-container">
      <chart-card class="chart-card-left-big">
        <div class="center">
          <h3>
            <hdd-outlined />
            Storage
          </h3>
        </div>
        <div v-if="loaded">
            <div v-if="chartLabels.length > 0">
              <resource-stats-line-chart
                :chartLabels="chartLabels"
                :chartData="resourceUsageHistory.storage"
                :yAxisInitialMax="50"
                :yAxisIncrementValue="1"
                :yAxisMeasurementUnit="'TB'"
              />
            </div>
          </div>
      </chart-card>
      <chart-card class="chart-card-right-little">
        <div class="center">
          <h3>
            <PieChartOutlined />
            OS Repartition
          </h3>
        </div>
      </chart-card>

    </div>
    <div class="chart-container">
      <chart-card class="chart-card-left-full">
        <div class="center">
          <h3>
            <wifi-outlined />
            Bandwith InOut
          </h3>
        </div>
      </chart-card>
    </div>
    <div class="chart-container">
      <chart-card class="chart-card-left-little">
        <div class="center">
          <a-statistic title="Actual VM Count" :value="93" >
          <template #suffix>
            <DesktopOutlined />
            </template>
          </a-statistic>
        </div>
      </chart-card>
      <chart-card class="chart-card-right-big">
        <div class="center">
          <h3>
            <LineChartOutlined />
            Historique Nb VM
          </h3>
        </div>
      </chart-card>
    </div>
    <div class="chart-container">
      <chart-card class="chart-card-left-big">
        <div class="center">
          <h3>
            <LineChartOutlined />
            Historique Nb VR
          </h3>
        </div>
      </chart-card>
      <chart-card class="chart-card-right-little">
        <div class="center">
          <a-statistic title="Actual VR Count" :value="93" >
          <template #suffix>
            <DesktopOutlined />
            </template>
          </a-statistic>
        </div>
      </chart-card>
    </div>
  </div>
</template>

<script>

import Breadcrumb from '@/components/widgets/Breadcrumb'
import ChartCard from '@/components/widgets/ChartCard'
import ResourceLayout from '@/layouts/ResourceLayout'
import ResourceStatsLineChart from '@/components/view/stats/ResourceStatsLineChart.vue'
import 'chartjs-adapter-moment'

export default {
  name: 'finops',
  components: {
    ChartCard,
    Breadcrumb,
    ResourceLayout,
    ResourceStatsLineChart
  },
  data () {
    return {
      loaded: false,
      chartLabels: [],
      resourceUsageHistory: {
        cpu: [],
        ram: [],
        storage: []
      }
    }
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.handleStatsResponse()
    },
    handleStatsResponse () {
      this.resetData()

      // const chartPointRadius = this.getChartPointRadius(vm[0].stats.length)
      const chartPointRadius = this.getChartPointRadius(60)

      const blue = '#166ab7'
      const green = '#389357'
      const blueInRgba = 'rgba(24, 144, 255, 0.5)'
      const greenInRgba = 'rgba(59, 198, 133, 0.65)'
      const red = '#ff4d4f'
      const redInRgba = 'rgb(255, 77, 79, 0.65)'

      const cpuTotal = { label: 'CPU Total', backgroundColor: blueInRgba, borderColor: red, data: [], pointRadius: chartPointRadius }
      const cpuUsed = { label: 'CPU Allocated', backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }
      const cpuWithoutOverprovisionning = { label: 'CPU without OverProvisionning', backgroundColor: greenInRgba, borderColor: green, data: [], pointRadius: chartPointRadius }

      const storageUsed = { label: 'Storage Allocated', backgroundColor: greenInRgba, borderColor: green, data: [], pointRadius: chartPointRadius }
      const storageTotal = { label: 'Storage Total', backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }
      const storageWithoutOverprovisionning = { label: 'Storage without OverProvisionning', backgroundColor: redInRgba, borderColor: red, data: [], pointRadius: chartPointRadius }

      const ramUsed = { label: 'RAM Used', backgroundColor: blueInRgba, borderColor: blue, data: [], pointRadius: chartPointRadius }
      const ramTotal = { label: 'RAM Total', backgroundColor: redInRgba, borderColor: red, data: [], pointRadius: chartPointRadius }
      const ramWithoutOverprovisionning = { label: 'RAM  without OverProvisionning', backgroundColor: greenInRgba, borderColor: green, data: [], pointRadius: chartPointRadius }

      // generate data
      const datas = []
      const timestamp = new Date()
      for (let i = 0; i < 60; i++) {
        timestamp.setHours(timestamp.getHours() + i)

        const data = {}

        data.timestamp = timestamp.toISOString()
        data.cpuTotal = 901
        data.cpuUsed = Math.random() * (1034 - 800) + 800
        data.cpuWithoutOverprovisionning = 2250.00

        data.ramTotal = 3.8
        data.ramUsed = Math.random() * (3 - 1.5) + 2.5
        data.ramWithoutOverprovisionning = 5

        data.storageTotal = 40
        data.storageUsed = Math.random() * (28 - 26) + 26
        data.storageWithoutOverprovisionning = 45

        datas.push(data)
      }
      console.log('datas', datas)
      if (datas.length > 0) {
        for (const data of datas) {
          var ts = this.$toLocalDate(data.timestamp)
          const currentLabel = ts.split('T')[0] + ' ' + ts.split('T')[1].split('-')[0]
          this.chartLabels.push(currentLabel)

          cpuUsed.data.push({ timestamp: currentLabel, stat: data.cpuUsed })
          cpuTotal.data.push({ timestamp: currentLabel, stat: data.cpuTotal })
          cpuWithoutOverprovisionning.data.push({ timestamp: currentLabel, stat: data.cpuWithoutOverprovisionning })

          ramUsed.data.push({ timestamp: currentLabel, stat: data.ramUsed })
          ramTotal.data.push({ timestamp: currentLabel, stat: data.ramTotal })
          ramWithoutOverprovisionning.data.push({ timestamp: currentLabel, stat: data.ramWithoutOverprovisionning })

          storageUsed.data.push({ timestamp: currentLabel, stat: data.storageUsed })
          storageTotal.data.push({ timestamp: currentLabel, stat: data.storageTotal })
          storageWithoutOverprovisionning.data.push({ timestamp: currentLabel, stat: data.storageWithoutOverprovisionning })
        }
        console.log('datas!!!')
        this.resourceUsageHistory.cpu.push(cpuUsed)
        this.resourceUsageHistory.cpu.push(cpuTotal)
        this.resourceUsageHistory.cpu.push(cpuWithoutOverprovisionning)

        this.resourceUsageHistory.storage.push(storageUsed)
        this.resourceUsageHistory.storage.push(storageTotal)
        this.resourceUsageHistory.storage.push(storageWithoutOverprovisionning)

        this.resourceUsageHistory.ram.push(ramUsed)
        this.resourceUsageHistory.ram.push(ramTotal)
        this.resourceUsageHistory.ram.push(ramWithoutOverprovisionning)
      }

      this.loaded = true
    },
    resetData () {
      console.log('reset')
      this.chartLabels = []
      this.resourceUsageHistory.cpu = []
      this.resourceUsageHistory.ram = []
      this.resourceUsageHistory.storage = []
      console.log('resetend')
    //   this.resourceUsageHistory.memory.percentage.free = []
    //   this.resourceUsageHistory.memory.percentage.used = []
    //   this.resourceUsageHistory.memory.rawData.free.inMB = []
    //   this.resourceUsageHistory.memory.rawData.free.inGB = []
    //   this.resourceUsageHistory.memory.rawData.used.inMB = []
    //   this.resourceUsageHistory.memory.rawData.used.inGB = []
    //   this.resourceUsageHistory.network.inKiB = []
    //   this.resourceUsageHistory.network.inMiB = []
    //   this.resourceUsageHistory.network.inGiB = []
    //   this.resourceUsageHistory.disk.iops = []
    //   this.resourceUsageHistory.disk.readAndWrite.inKiB = []
    //   this.resourceUsageHistory.disk.readAndWrite.inMiB = []
    //   this.resourceUsageHistory.disk.readAndWrite.inGiB = []
    },
    getChartPointRadius (numberOfDataPoints) {
      const maxSizeLimit = 3
      const minSizeLimit = 2
      const minSize = 0.1 // the smallest value that allows to render the point in the chart
      const result = (screen.width * 0.04) / numberOfDataPoints
      if (result > maxSizeLimit) {
        return maxSizeLimit
      } else if (result < minSizeLimit) {
        return minSize
      }
      return parseFloat(result).toFixed(2)
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/style/components/view/StatsTab.scss';
  .dashboard-card {
    width: 100%;
    min-height: 270px;
    margin-top: 20px;
  }

  .chart-container {
    display: inline-flex;
    width: 100%;
    min-height: 270px;
  }

  .chart-card-left {
    width: 50%;
    min-height: 270px;
    margin-top: 10px;
    margin-right: 10px;
  }

  .chart-card-left-big {
    width: 80%;
    min-height: 270px;
    margin-top: 10px;
    margin-right: 10px;
  }

  .chart-card-left-little {
    width: 20%;
    min-height: 270px;
    margin-top: 10px;
    margin-right: 5px;
  }

  .chart-card-left-full {
    width: 100%;
    min-height: 270px;
    margin-top: 10px;
    margin-right: 10px;
  }

  .chart-card-right {
    width: 50%;
    min-height: 270px;
    margin-top: 10px;
    margin-left: 10px;
  }

  .chart-card-right-big {
    width: 80%;
    min-height: 270px;
    margin-top: 10px;
    margin-left: 10px;
  }

  .chart-card-right-little {
    width: 20%;
    min-height: 270px;
    margin-top: 10px;
    margin-left: 5px;
  }

  .chart-card-right-full {
    width: 100%;
    min-height: 270px;
    margin-top: 10px;
    margin-left: 10px;
  }

  .filter-card {
    width: 100%;
    min-height: 80px;
    margin-bottom: 20px;
  }

  .breadcrumb-card {
    margin-left: -24px;
    margin-right: -24px;
    margin-top: -16px;
    margin-bottom: 12px;
  }

  .filter-container {
    display: inline-flex;
    width: 100%;
  }

  .select-container {
    margin-right: 20px;
  }

  .radio-container {
    align-items: right;
  }
</style>
