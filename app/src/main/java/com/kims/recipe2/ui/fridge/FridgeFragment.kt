package com.kims.recipe2.ui.fridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kims.recipe2.databinding.FragmentFridgeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FridgeFragment : Fragment() {

    private var _binding: FragmentFridgeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FridgeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFridgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSectionClickListeners()
        setupCategoryRecyclerView()
    }

    private fun setupSectionClickListeners() {
        binding.sectionFreezer.setOnClickListener { showItemsDialog("냉동실", viewModel.fridgeContents["freezer"]) }
        binding.sectionMain.setOnClickListener { showItemsDialog("냉장실", viewModel.fridgeContents["main"]) }
        binding.sectionVegetable.setOnClickListener { showItemsDialog("야채실", viewModel.fridgeContents["vegetable"]) }
        binding.sectionDoor.setOnClickListener { showItemsDialog("문짝", viewModel.fridgeContents["door"]) }
    }

    private fun setupCategoryRecyclerView() {
        val categoryAdapter = FridgeCategoryAdapter { category ->
            // 카테고리 클릭 시 상세 목록 화면으로 이동하는 로직 구현
            showItemsDialog(category.name, category.examples)
        }

        binding.rvFridgeCategories.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.submitList(categories)
        }
    }

    private fun showItemsDialog(title: String, items: String?) {
        if (items == null) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${title} 재료")
            .setMessage(items)
            .setPositiveButton("확인", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}