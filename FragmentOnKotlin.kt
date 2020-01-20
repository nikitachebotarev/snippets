package com.hudway.drive.core.widgets

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hudway.drive.HwApplication
import com.hudway.drive.R
import com.hudway.drive.core.settings.refactored.HwSettingsFragment
import com.hudway.drive.core.widgets.list.HwWidgetsInfoFragment
import com.hudway.drive.core.widgets.list.HwWidgetsListAdapter
import com.hudway.drive.nosorog.core.model.DashboardCenterWidget
import com.hudway.drive.nosorog.core.model.DashboardSideWidget
import com.hudway.drive.nosorog.ui.main.drive.edit.DashboardEditFragment
import com.hudway.drive.nosorog.ui.main.drive.edit.DriveFragmentType
import com.kivic.network.packet.command.HudWidgetCommandPacket

class HwWidgetsFragment : Fragment(), HwWidgetsContract.IHWWidgetsView,
    HwWidgetsListAdapter.IHwWidgetsListAdapterListener,
    DashboardEditFragment.DashboardEditFragmentListener,
    HwSettingsFragment.Listener,
    HwWidgetsInfoFragment.Listener {

    private lateinit var widgetsSettingListView: RecyclerView
    private lateinit var presenter: HwWidgetsPresenter
    private var widgetViewActionListener: Listener? = null

    private val settingsFragment: HwSettingsFragment = HwSettingsFragment()
    private lateinit var dashboardFragment: DashboardEditFragment
    private val infoFragment: HwWidgetsInfoFragment = HwWidgetsInfoFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = HwWidgetsPresenter(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.hw_fragment_widgets, container, false)

        infoFragment.listener = this

        widgetsSettingListView = view.findViewById(R.id.rv_widgets_settings)
        widgetsSettingListView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = HwWidgetsListAdapter(context, this@HwWidgetsFragment)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        presenter.attachView(this)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        presenter.attachView(this)
        presenter.viewIsReady()
    }

    override fun onPause() {
        super.onPause()
        presenter.deattachView()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.destroy()
    }

    override fun onClickedAtDashboardCell(type: Int) {
        widgetViewActionListener?.onDashboardEditScreenWillOpen() //INFO: закрыть панель в активити
        presenter.onDashboardCellClicked(type)
    }

    override fun onClickedAtDeviceInfoCell() {
        presenter.onDeviceInfoCellClicked()
    }

    override fun showDefaultNavigationState() {
        widgetViewActionListener?.onDashboardEditScreenWillClose()
        this.updateCellWithDeviceConnectInfo()
    }

    override fun updateCellWithDeviceConnectInfo() {
        if (this::widgetsSettingListView.isInitialized) {
            widgetsSettingListView.adapter?.notifyDataSetChanged()
        }
    }

    fun setWidgetViewActionListener(listener: Listener?) {
        widgetViewActionListener = listener
    }

    override fun onSettingsButtonClicked() {
        presenter.onSettingsButtonClicked()
    }

    override fun onCloseSettingsFragmentClicked() {
        activity?.supportFragmentManager?.beginTransaction()
            ?.remove(dashboardFragment)
            ?.commit()
        widgetViewActionListener?.onSettingsFragmentClose()
    }

    override fun onDashboardEditFragmentWillClose(
        type: Int,
        left: DashboardSideWidget,
        center: DashboardCenterWidget,
        right: DashboardSideWidget
    ) {
        activity?.supportFragmentManager?.beginTransaction()
            ?.remove(dashboardFragment)
            ?.commit()

        widgetViewActionListener?.onDashboardEditScreenWillClose()
        val adapter = HwWidgetsListAdapter(context!!, this)
        widgetsSettingListView.adapter = adapter

        Log.d(javaClass.simpleName, "sending widgets... $type $left $center $right")
        val packet = HudWidgetCommandPacket()
        packet.leftWidget = left.name
        packet.centerWidget = center.name
        packet.rightWidget = right.name
        packet.type = type
        HwApplication.instance.hudNetworkManager.sendPacket(packet)
    }


    override fun showSettingsScreen() {
        activity?.supportFragmentManager?.beginTransaction()
            ?.add(R.id.fragment_container, settingsFragment, HwSettingsFragment::class.java.name)
            ?.commit()
        widgetViewActionListener?.onSettingsFragmentClose()
    }

    override fun showDashboardScreen(type: Int) {
        dashboardFragment = DashboardEditFragment.newInstance(type, DriveFragmentType.Main, this)
        activity?.supportFragmentManager?.beginTransaction()
            ?.add(R.id.fragment_container, dashboardFragment, DashboardEditFragment::class.java.name)
            ?.commit()
        widgetViewActionListener?.onDashboardEditScreenWillOpen()
    }

    override fun onInfoButtonClicked() {
        activity?.supportFragmentManager?.beginTransaction()
            ?.add(R.id.fragment_container, infoFragment, HwWidgetsInfoFragment::class.java.name)
            ?.commit()
    }

    override fun onInfoFragmentClose() {
        activity?.supportFragmentManager?.beginTransaction()
            ?.remove(infoFragment)
            ?.commit()
    }

    interface Listener {

        fun onDashboardEditScreenWillOpen()
        fun onDashboardEditScreenWillClose()
        fun onSettingsFragmentOpen()
        fun onSettingsFragmentClose()
    }
}

interface IHudDevice {

    fun connectToDevice()
    fun disconnectDevice()
}
