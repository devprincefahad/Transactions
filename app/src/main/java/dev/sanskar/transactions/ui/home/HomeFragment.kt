package dev.sanskar.transactions.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.sanskar.transactions.*
import dev.sanskar.transactions.data.FilterByMediumChoices
import dev.sanskar.transactions.data.FilterByTypeChoices
import dev.sanskar.transactions.data.SortByChoices
import dev.sanskar.transactions.databinding.FragmentHomeBinding
import dev.sanskar.transactions.ui.model.MainViewModel

private const val TAG = "HomeFragment"
class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private val model by activityViewModels<MainViewModel>()
    private val adapter by lazy {
        TransactionsListAdapter(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_options_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_clear_transactions -> {
                // Clear transactions on dialog confirmation
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Are you sure?")
                    .setMessage("Are you sure you want to delete all transactions to date?")
                    .setPositiveButton("Yes") { _, _ ->
                        model.clearTransactions()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            R.id.action_dashboard -> {
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToDashboardFragment())
            }
            R.id.action_exchange_medium -> {
                findNavController().navigate(R.id.action_homeFragment_to_mediumExchangeFragment)
            }
            R.id.action_send_feedback -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setItems(arrayOf("GitHub", "Gmail")) { _, which ->
                        Log.d(TAG, "onOptionsItemSelected: $which")
                        when (which) {
                            0 -> {
                                Log.d(
                                    TAG,
                                    "onOptionsItemSelected: Attempting to create and launch a custom tab"
                                )
                            CustomTabsIntent.Builder()
                                .build()
                                .launchUrl(requireContext(), Uri.parse("https://github.com/sanskar10100/Transactions/issues/new"))
                            }
                            1 -> {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("sanskar10100@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, "Feedback - Transactions")
                                }
                                startActivity(Intent.createChooser(intent, "Send Feedback"))
                            }
                        }
                    }
                    .create()
                    .show()
            }
        }

        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAppUpdate()
        binding.listTransactions.adapter = adapter

        binding.fabAddTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addTransactionFragment)
        }

        model.transactions.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.textViewCashBalance.text = "₹${model.getCashBalance()}"
            binding.textViewDigitalBalance.text = "₹${model.getDigitalBalance()}"
        }

        setChipListeners()
    }

    override fun onResume() {
        super.onResume()
        setInitialChipTitles()
    }

    private fun setInitialChipTitles() {
        binding.chipSort.text = MainViewModel.QueryConfig.sortChoice.readableString
        binding.chipFilterType.text = MainViewModel.QueryConfig.filterTypeChoice.readableString
        binding.chipFilterMedium.text = MainViewModel.QueryConfig.filterMediumChoice.readableString
        binding.chipFilterAmount.text = MainViewModel.QueryConfig.filterAmountChoice.readableString
    }

    private fun setChipListeners() {
        // Sort Chip
        binding.chipSort.setOnClickListener {
            findNavController().navigate(
                generateOptionsDirection(
                    SortByChoices.values().map {
                        it.readableString
                    }.toTypedArray(), SORT_REQUEST_KEY, MainViewModel.QueryConfig.sortChoice.ordinal
                )
            )
            setFragmentResultListener(SORT_REQUEST_KEY) { _, bundle ->
                val selected = bundle.getInt(KEY_SELECTED_OPTION_INDEX)
                model.setSortMethod(selected)
                (it as Chip).text = MainViewModel.QueryConfig.sortChoice.readableString
            }
        }

        // Filter by Type Chip
        binding.chipFilterType.setOnClickListener {
            findNavController().navigate(
                generateOptionsDirection(
                    FilterByTypeChoices.values().map {
                        it.readableString
                    }.toTypedArray(), KEY_FILTER_BY_TYPE, MainViewModel.QueryConfig.filterTypeChoice.ordinal
                )
            )
            setFragmentResultListener(KEY_FILTER_BY_TYPE) { _, bundle ->
                val selected = bundle.getInt(KEY_SELECTED_OPTION_INDEX)
                model.setFilterType(selected)
                (it as Chip).text = MainViewModel.QueryConfig.filterTypeChoice.readableString
            }
        }

        // Filter by Medium Chip
        binding.chipFilterMedium.setOnClickListener {
            findNavController().navigate(
                generateOptionsDirection(
                    FilterByMediumChoices.values().map {
                        it.readableString
                    }.toTypedArray(), KEY_FILTER_BY_MEDIUM, MainViewModel.QueryConfig.filterMediumChoice.ordinal
                )
            )
            setFragmentResultListener(KEY_FILTER_BY_MEDIUM) { _, bundle ->
                val selected = bundle.getInt(KEY_SELECTED_OPTION_INDEX)
                model.setFilterMedium(selected)
                (it as Chip).text = MainViewModel.QueryConfig.filterMediumChoice.readableString
            }
        }

        binding.chipFilterAmount.setOnClickListener {
            val directions = HomeFragmentDirections.actionHomeFragmentToAmountFilterBottomSheet(
                MainViewModel.QueryConfig.filterAmountValue,
                MainViewModel.QueryConfig.filterAmountChoice.ordinal
            )
            findNavController().navigate(directions)
            setFragmentResultListener(KEY_FILTER_BY_AMOUNT) { _, bundle ->
                val amount = bundle.getInt(KEY_AMOUNT)
                val index = bundle.getInt(KEY_SELECTED_OPTION_INDEX)
                model.setFilterAmount(amount, index)
                (it as Chip).text = if (amount != -1)
                    "${MainViewModel.QueryConfig.filterAmountChoice.readableString} ${MainViewModel.QueryConfig.filterAmountValue}"
                    else MainViewModel.QueryConfig.filterAmountChoice.readableString
            }
        }
    }

    private fun generateOptionsDirection(options: Array<String>, key: String, selectedIndex: Int): NavDirections {
        return HomeFragmentDirections.actionHomeFragmentToOptionsBottomSheet(options, key, selectedIndex)
    }
}