import { Navigate, Route, Routes } from 'react-router-dom';
import './App.css';
import { UserProvider } from './context/UserContext';
import Landing from './pages/Landing';
import UserLayout from './layouts/UserLayout';
import UserHome from './pages/user/UserHome';
import LinkBankAccount from './pages/user/LinkBankAccount';
import AddPaymentMethod from './pages/user/AddPaymentMethod';
import MakePayment from './pages/user/MakePayment';
import MyPayments from './pages/user/MyPayments';
import BankLayout from './layouts/BankLayout';
import BankOverview from './pages/bank/BankOverview';
import PaymentsPage from './pages/bank/PaymentsPage';
import FeeRulesPage from './pages/bank/FeeRulesPage';
import RegisterUserPage from './pages/bank/RegisterUserPage';
import FlaggedTransactions from './pages/bank/FlaggedTransactions';

function App() {
  return (
    <UserProvider>
      <Routes>
        <Route path="/" element={<Landing />} />

        <Route path="/user" element={<UserLayout />}>
          <Route index element={<UserHome />} />
          <Route path="accounts" element={<LinkBankAccount />} />
          <Route path="methods" element={<AddPaymentMethod />} />
          <Route path="pay" element={<MakePayment />} />
          <Route path="payments" element={<MyPayments />} />
        </Route>

        <Route path="/bank" element={<BankLayout />}>
          <Route index element={<BankOverview />} />
          <Route path="payments" element={<PaymentsPage />} />
          <Route path="flagged" element={<FlaggedTransactions />} />
          <Route path="fee-rules" element={<FeeRulesPage />} />
          <Route path="users" element={<RegisterUserPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </UserProvider>
  );
}

export default App;

