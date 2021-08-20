import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nextgenbroadcast.mobile.middleware.scoreboard.databinding.FragmentCommandBinding

class CommandFragment : Fragment() {
    lateinit var binding: FragmentCommandBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCommandBinding.inflate(inflater, container, false)
        return binding.root
    }
}